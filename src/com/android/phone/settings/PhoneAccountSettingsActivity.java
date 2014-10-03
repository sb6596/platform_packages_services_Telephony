/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.phone.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.android.internal.telephony.PhoneConstants;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;

// TODO: Convert FeatureCallSettings into a preference fragment based activity, and load the
// PhoneAccountSettings fragment directly instead of starting this PreferenceActivity.
public class PhoneAccountSettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getActionBar().setTitle(R.string.phone_account_settings);
        getFragmentManager().beginTransaction().replace(
                android.R.id.content, new PhoneAccountSettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}