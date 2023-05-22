/*
 * ProjectGeneralPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectRVersion;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectGeneralPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectGeneralPreferencesPane(Session session)
   {
      sessionInfo_ = session.getSessionInfo();
      
      VerticalPanel container = new VerticalPanel();
      
      container.add(headerLabel("Workspace"));

      // use default label
      Label infoLabel = new Label(constants_.projectGeneralInfoLabel());
      infoLabel.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().infoLabel());
      container.add(lessSpaced(nudgeRightPlus(infoLabel)));

      // store workspace prefs in grid for nice alignment
      LayoutGrid grid = new LayoutGrid(3, 2);
      grid.addStyleName(RESOURCES.styles().workspaceGrid());

      // restore workspace
      restoreWorkspace_ = new YesNoAskDefault(false);
      grid.setWidget(0, 0, new FormLabel(constants_.restoreWorkspaceText(), restoreWorkspace_));
      grid.setWidget(0, 1, restoreWorkspace_);

      // save workspace
      saveWorkspace_ = new YesNoAskDefault(true);
      grid.setWidget(1, 0, new FormLabel(constants_.saveWorkspaceText(), saveWorkspace_));
      grid.setWidget(1, 1, saveWorkspace_);

      // always save history
      alwaysSaveHistory_ = new YesNoAskDefault(false);
      grid.setWidget(2, 0, new FormLabel(constants_.alwaysSaveHistoryText(), alwaysSaveHistory_));
      grid.setWidget(2, 1, alwaysSaveHistory_);
      
      container.add(grid);
      
      container.add(headerLabel("Miscellaneous"));

      // disable execute .Rprofile
      disableExecuteRprofile_ = new CheckBox(constants_.disableExecuteRprofileText());
      container.add(nudgeRight(disableExecuteRprofile_));

      // quit child processes
      quitChildProcessesOnExit_ = new CheckBox(constants_.quitChildProcessesOnExitText());
      container.add(nudgeRight(quitChildProcessesOnExit_));

      add(container);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconR2x());
   }

   @Override
   public String getName()
   {
      return constants_.generalText();
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      PackratContext context = options.getPackratContext();
      restoreWorkspace_.setSelectedIndex(config.getRestoreWorkspace());
      saveWorkspace_.setSelectedIndex(config.getSaveWorkspace());
      alwaysSaveHistory_.setSelectedIndex(config.getAlwaysSaveHistory());
      tutorialPath_ = config.getTutorialPath();
      rVersion_ = config.getRVersion();

      // if we are in packrat mode, disable the ability to set the disable execute Rprofile setting
      // the Rprofile always executes on session start in this case as it is required by packrat
      if (context != null && context.isModeOn())
      {
         disableExecuteRprofile_.setEnabled(false);
         disableExecuteRprofile_.setValue(false);
      }
      else
         disableExecuteRprofile_.setValue(config.getDisableExecuteRprofile());

      // check or uncheck the checkbox for child processes based on the configuration value
      // if default is specified, we need to use the current session setting
      // otherwise, yes or no indicate the check state exactly
      int quitChildProcessesOnExit = config.getQuitChildProcessesOnExit();
      boolean quitChildProcessesChecked = sessionInfo_.quitChildProcessesOnExit();

      switch (quitChildProcessesOnExit)
      {
      case YesNoAskDefault.YES_VALUE:
         quitChildProcessesChecked = true;
         break;
      case YesNoAskDefault.NO_VALUE:
         quitChildProcessesChecked = false;
         break;
      }

      quitChildProcessesOnExit_.setValue(quitChildProcessesChecked);

   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setRestoreWorkspace(restoreWorkspace_.getSelectedIndex());
      config.setSaveWorkspace(saveWorkspace_.getSelectedIndex());
      config.setAlwaysSaveHistory(alwaysSaveHistory_.getSelectedIndex());
      config.setTutorialPath(tutorialPath_);
      config.setRVersion(rVersion_);
      config.setDisableExecuteRprofile(disableExecuteRprofile_.getValue());

      // turn the quit child processes checkbox from a boolean into the
      // YesNoAsk value that it should be in the configuration
      boolean quitChildProcessesChecked = quitChildProcessesOnExit_.getValue();
      int quitChildProcessesOnExit = 0;
      if (quitChildProcessesChecked != sessionInfo_.quitChildProcessesOnExit())
      {
         quitChildProcessesOnExit = (quitChildProcessesChecked ? YesNoAskDefault.YES_VALUE : YesNoAskDefault.NO_VALUE);
      }

      config.setQuitChildProcessesOnExit(quitChildProcessesOnExit);
      return new RestartRequirement();
   }

   private YesNoAskDefault restoreWorkspace_;
   private YesNoAskDefault saveWorkspace_;
   private YesNoAskDefault alwaysSaveHistory_;
   private CheckBox disableExecuteRprofile_;
   private CheckBox quitChildProcessesOnExit_;
   private SessionInfo sessionInfo_;

   private String tutorialPath_;

   private RProjectRVersion rVersion_;
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
