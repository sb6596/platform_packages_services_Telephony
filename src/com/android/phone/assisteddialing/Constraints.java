/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.phone.assisteddialing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;
import com.android.phone.PhoneGlobals;

import java.util.Locale;
import java.util.Optional;

/**
 * Ensures that a number is eligible for Assisted Dialing
 */
final class Constraints {
    private final PhoneNumberUtil mPhoneNumberUtil = PhoneGlobals.getInstance()
            .getPhoneNumberUtil();
    private final Context mContext;
    private final CountryCodeProvider mCountryCodeProvider;
    private static final String LOG_TAG = "Constraints";

    /**
     * Create a new instance of Constraints.
     *
     * @param mContext                   The mContext used to determine whether or not a number
     *                                   is an emergency number.
     * @param configProviderCountryCodes A csv of supported country codes, e.g. "US,CA"
     */
    Constraints(@NonNull Context context, @NonNull CountryCodeProvider countryCodeProvider) {
        this.mContext = context;
        this.mCountryCodeProvider = countryCodeProvider;
    }

    /**
     * Determines whether or not we think Assisted Dialing is possible.
     *
     * @param numberToCheck          A string containing the phone number.
     * @param userHomeCountryCode    A string containing an ISO 3166-1 alpha-2 country code
     *                               representing the user's home country.
     * @param userRoamingCountryCode A string containing an ISO 3166-1 alpha-2 country code
     *                               representing the user's roaming country.
     * @return A boolean indicating whether or not the provided values are eligible for assisted
     * dialing.
     */
    public boolean meetsPreconditions(
            @NonNull String numberToCheck,
            @NonNull String userHomeCountryCode,
            @NonNull String userRoamingCountryCode) {
        if (this.mContext == null) {
            Log.i(LOG_TAG, "Provided context cannot be null");
            return false;
        }

        if (this.mCountryCodeProvider == null) {
            Log.i(LOG_TAG, "Provided configProviderCountryCodes cannot be null");
            return false;
        }

        if (TextUtils.isEmpty(numberToCheck)) {
            Log.i(LOG_TAG, "numberToCheck was empty");
            return false;
        }

        if (TextUtils.isEmpty(userHomeCountryCode)) {
            Log.i(LOG_TAG, "userHomeCountryCode was empty");
            return false;
        }

        if (TextUtils.isEmpty(userRoamingCountryCode)) {
            Log.i(LOG_TAG, "userRoamingCountryCode was empty");
            return false;
        }

        userHomeCountryCode = userHomeCountryCode.toUpperCase(Locale.US);
        userRoamingCountryCode = userRoamingCountryCode.toUpperCase(Locale.US);

        Optional<PhoneNumber> parsedPhoneNumber = parsePhoneNumber(numberToCheck,
                userHomeCountryCode);

        if (!parsedPhoneNumber.isPresent()) {
            Log.i(LOG_TAG, "parsedPhoneNumber was empty");
            return false;
        }

        return areSupportedCountryCodes(userHomeCountryCode, userRoamingCountryCode)
                && isUserRoaming(userHomeCountryCode, userRoamingCountryCode)
                && !isInternationalNumber(parsedPhoneNumber)
                && !isEmergencyNumber(numberToCheck, mContext)
                && isValidNumber(parsedPhoneNumber)
                && !hasExtension(parsedPhoneNumber);
    }

    /**
     * Returns a boolean indicating the value equivalence of the provided country codes.
     */
    private boolean isUserRoaming(
            @NonNull String userHomeCountryCode, @NonNull String userRoamingCountryCode) {
        boolean result = !userHomeCountryCode.equals(userRoamingCountryCode);
        Log.i(LOG_TAG, "isUserRoaming = " + String.valueOf(result));
        return result;
    }

    /**
     * Returns a boolean indicating the support of both provided country codes for assisted dialing.
     * Both country codes must be allowed for the return value to be true.
     */
    private boolean areSupportedCountryCodes(
            @NonNull String userHomeCountryCode, @NonNull String userRoamingCountryCode) {
        if (TextUtils.isEmpty(userHomeCountryCode)) {
            Log.i(LOG_TAG, "userHomeCountryCode was empty");
            return false;
        }

        if (TextUtils.isEmpty(userRoamingCountryCode)) {
            Log.i(LOG_TAG, "userRoamingCountryCode was empty");
            return false;
        }

        boolean result =
                mCountryCodeProvider.isSupportedCountryCode(userHomeCountryCode)
                        && mCountryCodeProvider.isSupportedCountryCode(userRoamingCountryCode);
        Log.i("Constraints.areSupportedCountryCodes", String.valueOf(result));
        return result;
    }

    /**
     * A convenience method to take a number as a String and a specified country code, and return a
     * PhoneNumber object.
     */
    private Optional<PhoneNumber> parsePhoneNumber(
            @NonNull String numberToParse, @NonNull String userHomeCountryCode) {
        try {
            return Optional.of(mPhoneNumberUtil.parseAndKeepRawInput(numberToParse,
                    userHomeCountryCode));
        } catch (NumberParseException e) {
            Log.i(LOG_TAG, "could not parse the number");
            return Optional.empty();
        }
    }

    /**
     * Returns a boolean indicating if the provided number is already internationally formatted.
     */
    private boolean isInternationalNumber(@NonNull Optional<PhoneNumber> parsedPhoneNumber) {

        if (parsedPhoneNumber.get().hasCountryCode()
                && parsedPhoneNumber.get().getCountryCodeSource()
                != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
            Log.i(
                    LOG_TAG,
                    "phone number already provided the country code");
            return true;
        }
        return false;
    }

    /**
     * Returns a boolean indicating if the provided number has an extension.
     * <p>
     * <p>Extensions are currently stripped when formatting a number for mobile dialing, so we don't
     * want to purposefully truncate a number.
     */
    private boolean hasExtension(@NonNull Optional<PhoneNumber> parsedPhoneNumber) {

        if (parsedPhoneNumber.get().hasExtension()
                && !TextUtils.isEmpty(parsedPhoneNumber.get().getExtension())) {
            Log.i(LOG_TAG, "phone number has an extension");
            return true;
        }
        return false;
    }

    /**
     * Returns a boolean indicating if the provided number is considered to be a valid number.
     */
    private boolean isValidNumber(@NonNull Optional<PhoneNumber> parsedPhoneNumber) {
        boolean result = PhoneNumberUtil.getInstance().isValidNumber(parsedPhoneNumber.get());
        Log.i(LOG_TAG, "isValidNumber = " + String.valueOf(result));

        return result;
    }

    /**
     * Returns a boolean indicating if the provided number is an emergency number.
     */
    private boolean isEmergencyNumber(@NonNull String numberToCheck, @NonNull Context mContext) {
        // isEmergencyNumber may depend on network state, so also use isLocalEmergencyNumber when
        // roaming and out of service.
        boolean result =
                PhoneNumberUtils.isEmergencyNumber(numberToCheck)
                        || PhoneNumberUtils.isLocalEmergencyNumber(mContext, numberToCheck);
        Log.i("Constraints.isEmergencyNumber", String.valueOf(result));
        return result;
    }
}