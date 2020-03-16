/*
 * footnote-transaction.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { Fragment, Node as ProsemirrorNode } from 'prosemirror-model';
import { Transaction, EditorState, TextSelection } from 'prosemirror-state';
import { findChildrenByType, NodeWithPos, findSelectedNodeOfType, findParentNode } from 'prosemirror-utils';

import { uuidv4 } from '../../api/util';

import { findNoteNode, selectedNote } from './footnote';

// examine transactions and filter out attempts to place foonotes within note bodies
// (this is not allowed by pandoc markdown)
export function footnoteFilterTransaction() {
  return (tr: Transaction, state: EditorState) => {
    const noteWithPos = selectedNote(tr.selection);
    if (noteWithPos && findChildrenByType(noteWithPos.node, state.schema.nodes.footnote).length) {
      return false;
    }
    return true;
  };
}

// examine editor transactions and append a transaction that handles fixup of footnote numbers,
// importing of pasted footnotes, selection propagation to the footnote editor, etc.
export function footnoteAppendTransaction() {
  const footnoteChange = (node: ProsemirrorNode) => {
    const schema = node.type.schema;
    return node.type === schema.nodes.footnote || node.type === schema.nodes.note;
  };

  return {
    name: 'footnote-renumber',
    nodeFilter: footnoteChange,
    append: (tr: Transaction) => {
      // alias schema
      const schema = tr.doc.type.schema;

      // query for notes and footnotes
      const footnotes = findAllFootnotes(tr.doc);
      const allNotes = findAllNotes(tr.doc);

      // iterate through footnotes in the newState
      const refs = new Set<string>();
      footnotes.forEach((footnote, index) => {
        // footnote number
        const number = index + 1;

        // alias ref and content (either or both may be updated)
        let { ref, content } = footnote.node.attrs;

        // we may be creating a new note to append
        let newNote: any;

        // get reference to note (if any)
        const note = allNotes.find(noteWithPos => noteWithPos.node.attrs.ref === ref);

        // matching note found
        if (note) {
          // map position since we scanned for all of the notes at the top
          note.pos = tr.mapping.map(note.pos);

          // update content if this is a note edit (used to propagate user edits back to data-content)
          if (findParentNode(node => node.type === schema.nodes.note && node.attrs.ref === ref)(tr.selection)) {
            content = JSON.stringify(note.node.content.toJSON());
          }

          // if we've already processed this ref then it's a duplicate, make a copy w/ a new ref/id
          if (refs.has(ref)) {
            // create a new unique id and change the ref to it
            ref = uuidv4();

            // create and insert new note with this id
            newNote = schema.nodes.note.createAndFill({ ref, number }, note.node.content);

            // otherwise update the note with the correct number (if necessary)
          } else {
            if (note.node.attrs.number !== number) {
              tr.setNodeMarkup(note.pos, schema.nodes.note, {
                ...note.node.attrs,
                number,
              });
            }
          }

          // if there is no note then create one using the content attr
          // (this can happen for a copy/paste operation from another document)
        } else if (content) {
          newNote = schema.nodes.note.createAndFill({ ref, number }, Fragment.fromJSON(schema, JSON.parse(content)));
        }

        // insert newNote if necessary
        if (newNote) {
          const notesContainer = findNotesContainer(tr.doc);
          tr.insert(notesContainer.pos + 1, newNote as ProsemirrorNode);
        }

        // indicate that we've seen this ref
        refs.add(ref);

        // set new footnote markup if necessary
        const attrs = footnote.node.attrs;
        if (ref !== attrs.ref || content !== attrs.content || number !== attrs.number) {
          tr.setNodeMarkup(footnote.pos, schema.nodes.footnote, {
            ...footnote.node.attrs,
            ref,
            content,
            number,
          });
        }
      });

      // remove ophraned notes (backwards so the positions stay valid)
      for (let i = allNotes.length - 1; i >= 0; i--) {
        const note = allNotes[i];
        const footnote = footnotes.find(fn => fn.node.attrs.ref === note.node.attrs.ref);
        if (!footnote) {
          tr.delete(note.pos, note.pos + note.node.nodeSize);
        }
      }
    },
  };
}

export function footnoteSelectNoteAppendTransaction() {
  return (_transactions: Transaction[], _oldState: EditorState, newState: EditorState) => {
    const schema = newState.schema;
    const footnoteNode: NodeWithPos | undefined = findSelectedNodeOfType(schema.nodes.footnote)(newState.selection);
    if (footnoteNode) {
      const tr = newState.tr;
      const ref = footnoteNode.node.attrs.ref;
      const noteNode = findNoteNode(tr.doc, ref);
      if (noteNode) {
        tr.setSelection(TextSelection.near(tr.doc.resolve(noteNode.pos)));
      }
      return tr;
    }
  };
}

function findAllFootnotes(doc: ProsemirrorNode) {
  const schema = doc.type.schema;
  return findChildrenByType(doc, schema.nodes.footnote, true);
}

function findAllNotes(doc: ProsemirrorNode) {
  const schema = doc.type.schema;
  const notesContainer = findNotesContainer(doc);
  const offset = notesContainer.pos + 1;
  const notes = findChildrenByType(notesContainer.node, schema.nodes.note, false);
  return notes.map(note => ({ ...note, pos: note.pos + offset }));
}

function findNotesContainer(doc: ProsemirrorNode) {
  return findChildrenByType(doc, doc.type.schema.nodes.notes, false)[0];
}
