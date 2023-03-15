#!/usr/bin/env bash
#
# RStudio Release Notarization (notarize-release.sh)
# 
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
# Notarizes an RStudio release using the public Apple notary service.
#
# Usage: 
#
#    notarize-release.sh [path-to-dmg]
#
# where path-to-dmg is a relative or absolute path to the DMG file to notarize.
#
# Expects the following environment variables to be present:
#  
#    APPLE_ID: the ID of the Apple account under which to submit the
#      notarization request
#    APPLE_ID_PASSWORD: An app-specific password (NOT the primary password) for
#      the Apple ID. See details here: https://support.apple.com/en-us/HT204397
#  

if [[ "$#" -lt 1 ]]; then
    echo "Usage: notarize-release.sh [path-to-dmg]"
    exit 1
fi

# Validate environment vars
if [ -z "$APPLE_ID" ]; then
    echo "Please set the environment variable APPLE_ID to the AppleID under which to submit the notarization request."
    exit 1
fi
if [ -z "$APPLE_ID_PASSWORD" ]; then
    echo "Please set the environment variable APPLE_ID_PASSWORD to the password to the account named in the APPLE_ID environment variable."
    exit 1
fi

# Submit the notarization request to Apple
echo "Submitting notarization request using account $APPLE_ID..."
xcrun notarytool submit --wait \
    --apple-id $APPLE_ID \
    --password "@env:APPLE_ID_PASSWORD" \
    --team-id FYF2F5GFX4 \
    --progress \
    $1

# Check result
if [ $? -eq 0 ]; then
    echo "Notarization completed successfully."
else
    echo "Notarization failed."
    exit 1
fi

# Staple the result to DMG file (allows offline verification of notarization)
xcrun stapler staple $1
