package org.smartregister.anc.library.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.rules.RuleConstant;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jeasy.rules.api.Facts;
import org.joda.time.LocalDate;
import org.joda.time.Weeks;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.anc.library.AncLibrary;
import org.smartregister.anc.library.R;
import org.smartregister.anc.library.activity.BaseContactActivity;
import org.smartregister.anc.library.activity.BaseHomeRegisterActivity;
import org.smartregister.anc.library.activity.ContactJsonFormActivity;
import org.smartregister.anc.library.activity.ContactSummaryFinishActivity;
import org.smartregister.anc.library.activity.MainContactActivity;
import org.smartregister.anc.library.activity.ProfileActivity;
import org.smartregister.anc.library.domain.ButtonAlertStatus;
import org.smartregister.anc.library.domain.Contact;
import org.smartregister.anc.library.event.BaseEvent;
import org.smartregister.anc.library.model.ContactModel;
import org.smartregister.anc.library.model.PartialContact;
import org.smartregister.anc.library.rule.AlertRule;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.view.activity.DrishtiApplication;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by ndegwamartin on 14/03/2018.
 */

public class Utils extends org.smartregister.util.Utils {

    public static final SimpleDateFormat DB_DF = new SimpleDateFormat(ConstantsUtils.SQLITE_DATE_TIME_FORMAT);
    public static final SimpleDateFormat CONTACT_DF = new SimpleDateFormat(ConstantsUtils.CONTACT_DATE_FORMAT);
    public static final SimpleDateFormat CONTACT_SUMMARY_DF = new SimpleDateFormat(ConstantsUtils.CONTACT_SUMMARY_DATE_FORMAT);
    public static final ArrayList<String> ALLOWED_LEVELS;
    public static final String DEFAULT_LOCATION_LEVEL = "Health Facility";
    public static final String FACILITY = "Facility";
    public static final String HOME_ADDRESS = "Home Address";
    private static final DateTimeFormatter SQLITE_DATE_DF = DateTimeFormat.forPattern(ConstantsUtils.SQLITE_DATE_TIME_FORMAT);
    private static final String TAG = "Anc Utils";
    private static final String OTHER_SUFFIX = ", other]";

    static {
        ALLOWED_LEVELS = new ArrayList<>();
        ALLOWED_LEVELS.add(DEFAULT_LOCATION_LEVEL);
        ALLOWED_LEVELS.add(FACILITY);
    }

    public static void saveLanguage(String language) {
        Utils.getAllSharedPreferences().saveLanguagePreference(language);
        setLocale(new Locale(language));
    }

    public static void setLocale(Locale locale) {
        Resources resources = AncLibrary.getInstance().getApplicationContext().getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            AncLibrary.getInstance().getApplicationContext().createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, displayMetrics);
        }
    }

    public static String getLanguage() {
        return Utils.getAllSharedPreferences().fetchLanguagePreference();
    }

    public static void postEvent(BaseEvent event) {
        EventBus.getDefault().post(event);
    }

    public static void postStickyEvent(
            BaseEvent event) {//Each Sticky event must be manually cleaned by calling Utils.removeStickyEvent
        // after
        // handling
        EventBus.getDefault().postSticky(event);
    }

    public static void removeStickyEvent(BaseEvent event) {
        EventBus.getDefault().removeStickyEvent(event);

    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void writePrefString(Context context, final String key, final String value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static int convertDpToPx(Context context, int dp) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return Math.round(px);
    }

    public static boolean isEmptyMap(Map map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmptyCollection(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static String getTodaysDate() {
        return convertDateFormat(Calendar.getInstance().getTime(), DB_DF);
    }

    public static String convertDateFormat(Date date, SimpleDateFormat formatter) {

        return formatter.format(date);
    }

    @Nullable
    public static int getAttributeDrawableResource(Context context, int attributeId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        return typedValue.resourceId;
    }

    public static int getProfileImageResourceIdentifier() {
        return R.drawable.avatar_woman;
    }

    public static List<String> getListFromString(String stringArray) {
        List<String> stringList = new ArrayList<>();
        if (!StringUtils.isEmpty(stringArray)) {
            stringList = new ArrayList<>(
                    Arrays.asList(stringArray.substring(1, stringArray.length() - 1).replaceAll("\"", "").split(", ")));
        }
        return stringList;
    }

    /**
     * Check for the quick check form then finds whether it still has pending required fields, If it has pending fields if so
     * it redirects to the quick check page. If not pending required fields then it redirects to the main contact page
     *
     * @param baseEntityId       {@link String}
     * @param personObjectClient {@link CommonPersonObjectClient}
     * @param context            {@link Context}
     * @author martinndegwa
     */
    public static void proceedToContact(String baseEntityId, HashMap<String, String> personObjectClient, Context context) {
        try {

            Intent intent = new Intent(context.getApplicationContext(), ContactJsonFormActivity.class);

            Contact quickCheck = new Contact();
            quickCheck.setName(context.getResources().getString(R.string.quick_check));
            quickCheck.setFormName(ConstantsUtils.JsonFormUtils.ANC_QUICK_CHECK);
            quickCheck.setContactNumber(Integer.valueOf(personObjectClient.get(DBConstantsUtils.KeyUtils.NEXT_CONTACT)));
            quickCheck.setBackground(R.drawable.quick_check_bg);
            quickCheck.setActionBarBackground(R.color.quick_check_red);
            quickCheck.setBackIcon(R.drawable.ic_clear);
            quickCheck.setWizard(false);
            quickCheck.setHideSaveLabel(true);

            //partial contact exists?
            PartialContact partialContactRequest = new PartialContact();
            partialContactRequest.setBaseEntityId(baseEntityId);
            partialContactRequest.setContactNo(quickCheck.getContactNumber());
            partialContactRequest.setType(quickCheck.getFormName());

            String locationId = AncLibrary.getInstance().getContext().allSharedPreferences()
                    .getPreference(AllConstants.CURRENT_LOCATION_ID);

            ContactModel baseContactModel = new ContactModel();
            JSONObject form = baseContactModel.getFormAsJson(quickCheck.getFormName(), baseEntityId, locationId);

            String processedForm = ContactJsonFormUtils.getFormJsonCore(partialContactRequest, form).toString();

            if (hasPendingRequiredFields(new JSONObject(processedForm))) {
                intent.putExtra(ConstantsUtils.JsonFormExtraUtils.JSON, processedForm);
                intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, quickCheck);
                intent.putExtra(ConstantsUtils.IntentKeyUtils.BASE_ENTITY_ID, partialContactRequest.getBaseEntityId());
                intent.putExtra(ConstantsUtils.IntentKeyUtils.CLIENT_MAP, personObjectClient);
                intent.putExtra(ConstantsUtils.IntentKeyUtils.FORM_NAME, partialContactRequest.getType());
                intent.putExtra(ConstantsUtils.IntentKeyUtils.CONTACT_NO, partialContactRequest.getContactNo());
                Activity activity = (Activity) context;
                activity.startActivityForResult(intent, org.smartregister.anc.library.util.JsonFormUtils.REQUEST_CODE_GET_JSON);
            } else {
                intent = new Intent(context, MainContactActivity.class);
                intent.putExtra(ConstantsUtils.IntentKeyUtils.BASE_ENTITY_ID, baseEntityId);
                intent.putExtra(ConstantsUtils.IntentKeyUtils.CLIENT_MAP, personObjectClient);
                intent.putExtra(ConstantsUtils.IntentKeyUtils.FORM_NAME, partialContactRequest.getType());
                intent.putExtra(ConstantsUtils.IntentKeyUtils.CONTACT_NO,
                        Integer.valueOf(personObjectClient.get(DBConstantsUtils.KeyUtils.NEXT_CONTACT)));
                context.startActivity(intent);
            }


        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Utils.showToast(context,
                    "Error proceeding to contact for client " + personObjectClient.get(DBConstantsUtils.KeyUtils.FIRST_NAME));
        }
    }

    /**
     * Checks the pending required fields on the json forms and returns true|false
     *
     * @param object {@link JSONObject}
     * @return true|false {@link Boolean}
     * @throws Exception
     * @author martinndegwa
     */
    public static boolean hasPendingRequiredFields(JSONObject object) throws Exception {
        if (object != null) {
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith(RuleConstant.STEP)) {
                    JSONArray stepArray = object.getJSONObject(key).getJSONArray(JsonFormConstants.FIELDS);

                    for (int i = 0; i < stepArray.length(); i++) {
                        JSONObject fieldObject = stepArray.getJSONObject(i);
                        ContactJsonFormUtils.processSpecialWidgets(fieldObject);

                        boolean isRequiredField = org.smartregister.anc.library.util.JsonFormUtils.isFieldRequired(fieldObject);
                        //Do not check for required for fields that are invisible
                        if (fieldObject.has(JsonFormConstants.IS_VISIBLE) && !fieldObject.getBoolean(JsonFormConstants.IS_VISIBLE)) {
                            isRequiredField = false;
                        }

                        if (isRequiredField && ((fieldObject.has(JsonFormConstants.VALUE) && TextUtils.isEmpty(
                                fieldObject.getString(JsonFormConstants.VALUE))) || !fieldObject.has(JsonFormConstants.VALUE))) {
                            //TO DO Remove/ Alter logical condition
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * This finalizes the form and redirects you to the contact summary page for more confirmation of the data added
     *
     * @param context {@link Activity}
     * @author martinndegwa
     */
    public static void finalizeForm(Activity context, HashMap<String, String> womanDetails, boolean isRefferal) {
        try {

            Intent contactSummaryFinishIntent = new Intent(context, ContactSummaryFinishActivity.class);
            contactSummaryFinishIntent
                    .putExtra(ConstantsUtils.IntentKeyUtils.BASE_ENTITY_ID, womanDetails.get(DBConstantsUtils.KeyUtils.BASE_ENTITY_ID));
            contactSummaryFinishIntent.putExtra(ConstantsUtils.IntentKeyUtils.CLIENT_MAP, womanDetails);
            contactSummaryFinishIntent.putExtra(ConstantsUtils.IntentKeyUtils.CONTACT_NO,
                    Integer.valueOf(womanDetails.get(DBConstantsUtils.KeyUtils.NEXT_CONTACT)));
            if (isRefferal) {
                int contactNo = Integer.parseInt(womanDetails.get(DBConstantsUtils.KeyUtils.NEXT_CONTACT));
                if (contactNo < 0) {
                    contactSummaryFinishIntent.putExtra(ConstantsUtils.IntentKeyUtils.CONTACT_NO, Integer.valueOf(contactNo));
                } else {
                    contactSummaryFinishIntent.putExtra(ConstantsUtils.IntentKeyUtils.CONTACT_NO, Integer.valueOf("-" + contactNo));
                }
            } else {
                contactSummaryFinishIntent.putExtra(ConstantsUtils.IntentKeyUtils.CONTACT_NO,
                        Integer.valueOf(womanDetails.get(DBConstantsUtils.KeyUtils.NEXT_CONTACT)));
            }
            context.startActivity(contactSummaryFinishIntent);
        } catch (Exception e) {
            Log.e(BaseContactActivity.class.getCanonicalName(), e.getMessage());
        }

    }

    public static String fillTemplate(String stringValue, Facts facts) {
        String stringValueResult = stringValue;
        while (stringValueResult.contains("{")) {
            String key = stringValueResult.substring(stringValueResult.indexOf("{") + 1, stringValueResult.indexOf("}"));
            String value = processValue(key, facts);
            stringValueResult = stringValueResult.replace("{" + key + "}", value).replaceAll(", $", "").trim();
        }
        //Remove unnecessary commas by cleaning the returned string
        return cleanValueResult(stringValueResult);
    }

    private static String processValue(String key, Facts facts) {
        String value = "";
        if (facts.get(key) instanceof String) {
            value = facts.get(key);
            if (value != null && value.endsWith(OTHER_SUFFIX)) {
                Object otherValue = value.endsWith(OTHER_SUFFIX) ? facts.get(key + ConstantsUtils.SuffixUtils.OTHER) : "";
                value = otherValue != null ?
                        value.substring(0, value.lastIndexOf(",")) + ", " + otherValue.toString() + "]" :
                        value.substring(0, value.lastIndexOf(",")) + "]";

            }
        }

        return ContactJsonFormUtils.keyToValueConverter(value);
    }

    private static String cleanValueResult(String result) {
        List<String> nonEmptyItems = new ArrayList<>();

        for (String item : result.split(",")) {
            if (item.length() > 1) {
                nonEmptyItems.add(item);
            }
        }
        //Get the first item that usually  has a colon and remove it form list, if list has one item append separator
        String itemLabel = "";
        if (!nonEmptyItems.isEmpty() && nonEmptyItems.get(0).contains(":")) {
            String[] separatedLabel = nonEmptyItems.get(0).split(":");
            itemLabel = separatedLabel[0];
            if (separatedLabel.length > 1) {
                nonEmptyItems.set(0, nonEmptyItems.get(0).split(":")[1]);
            }//replace with extracted value
        }
        return itemLabel + (!TextUtils.isEmpty(itemLabel) ? ": " : "") + StringUtils.join(nonEmptyItems.toArray(), ",");
    }

    public static void navigateToHomeRegister(Context context, boolean isRemote, Class<? extends BaseHomeRegisterActivity> homeRegisterActivityClass) {
        Intent intent = new Intent(context, homeRegisterActivityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ConstantsUtils.IntentKeyUtils.IS_REMOTE_LOGIN, isRemote);
        context.startActivity(intent);
    }

    public static void navigateToProfile(Context context, HashMap<String, String> patient) {

        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra(ConstantsUtils.IntentKeyUtils.BASE_ENTITY_ID, patient.get(DBConstantsUtils.KeyUtils.ID_LOWER_CASE));
        intent.putExtra(ConstantsUtils.IntentKeyUtils.CLIENT_MAP, patient);
        context.startActivity(intent);
    }

    public static String getColumnMapValue(CommonPersonObjectClient pc, String key) {
        return org.smartregister.util.Utils.getValue(pc.getColumnmaps(), key, false);
    }

    public static String getDBDateToday() {
        return (new LocalDate()).toString(SQLITE_DATE_DF);
    }

    public static ButtonAlertStatus getButtonAlertStatus(Map<String, String> details, String textTemplate) {

        String contactStatus = details.get(DBConstantsUtils.KeyUtils.CONTACT_STATUS);

        String nextContactDate = details.get(DBConstantsUtils.KeyUtils.NEXT_CONTACT_DATE);
        String edd = details.get(DBConstantsUtils.KeyUtils.EDD);
        String buttonAlertStatus;
        Integer gestationAge = 0;
        if (StringUtils.isNotBlank(edd)) {
            gestationAge = Utils.getGestationAgeFromEDDate(edd);
            AlertRule alertRule = new AlertRule(gestationAge, nextContactDate);
            buttonAlertStatus =
                    StringUtils.isNotBlank(contactStatus) && ConstantsUtils.AlertStatusUtils.ACTIVE.equals(contactStatus) ?
                            ConstantsUtils.AlertStatusUtils.IN_PROGRESS : AncLibrary.getInstance().getAncRulesEngineHelper()
                            .getButtonAlertStatus(alertRule, ConstantsUtils.RulesFileUtils.ALERT_RULES);
        } else {
            buttonAlertStatus = StringUtils.isNotBlank(contactStatus) ? ConstantsUtils.AlertStatusUtils.IN_PROGRESS : "DEAD";
        }

        ButtonAlertStatus buttonAlertStatus1 = new ButtonAlertStatus();

        //Set text first
        String nextContactRaw = details.get(DBConstantsUtils.KeyUtils.NEXT_CONTACT);
        Integer nextContact = StringUtils.isNotBlank(nextContactRaw) ? Integer.valueOf(nextContactRaw) : 1;

        nextContactDate =
                StringUtils.isNotBlank(nextContactDate) ? Utils.reverseHyphenSeperatedValues(nextContactDate, "/") : null;

        buttonAlertStatus1.buttonText = String.format(textTemplate, nextContact, (nextContactDate != null ? nextContactDate :
                Utils.convertDateFormat(Calendar.getInstance().getTime(), Utils.CONTACT_DF)));


        buttonAlertStatus =
                Utils.processContactDoneToday(details.get(DBConstantsUtils.KeyUtils.LAST_CONTACT_RECORD_DATE), buttonAlertStatus);

        buttonAlertStatus1.buttonAlertStatus = buttonAlertStatus;
        buttonAlertStatus1.gestationAge = gestationAge;
        buttonAlertStatus1.nextContact = nextContact;
        buttonAlertStatus1.nextContactDate = nextContactDate;

        return buttonAlertStatus1;
    }

    public static int getGestationAgeFromEDDate(String expectedDeliveryDate) {
        try {
            if (!"0".equals(expectedDeliveryDate)) {
                LocalDate date = SQLITE_DATE_DF.withOffsetParsed().parseLocalDate(expectedDeliveryDate);
                LocalDate lmpDate = date.minusWeeks(ConstantsUtils.DELIVERY_DATE_WEEKS);
                Weeks weeks = Weeks.weeksBetween(lmpDate, LocalDate.now());
                return weeks.getWeeks();
            } else {
                return 0;
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage(), e);
            return 0;
        }
    }

    public static String reverseHyphenSeperatedValues(String rawString, String outputSeparator) {
        if (!TextUtils.isEmpty(rawString) && rawString != null) {
            String resultString = rawString;
            String[] tokenArray = resultString.trim().split("-");
            ArrayUtils.reverse(tokenArray);
            resultString = StringUtils.join(tokenArray, outputSeparator);
            return resultString;
        }
        return "";
    }

    public static String processContactDoneToday(String lastContactDate, String alertStatus) {
        String result = alertStatus;

        if (!TextUtils.isEmpty(lastContactDate)) {

            try {
                result = DateUtils.isToday(DB_DF.parse(lastContactDate).getTime()) ? ConstantsUtils.AlertStatusUtils.TODAY :
                        alertStatus;
            } catch (ParseException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        return result;
    }

    public static void processButtonAlertStatus(Context context, Button dueButton, TextView contactTextView,
                                                ButtonAlertStatus buttonAlertStatus) {

        dueButton.setVisibility(View.VISIBLE);
        dueButton.setText(buttonAlertStatus.buttonText);
        dueButton.setTag(R.id.GESTATION_AGE, buttonAlertStatus.gestationAge);

        if (buttonAlertStatus.buttonAlertStatus != null) {
            switch (buttonAlertStatus.buttonAlertStatus) {
                case ConstantsUtils.AlertStatusUtils.IN_PROGRESS:
                    contactTextView.setVisibility(View.GONE);
                    dueButton.setBackgroundColor(context.getResources().getColor(R.color.progress_orange));
                    dueButton.setTextColor(context.getResources().getColor(R.color.white));
                    break;
                case ConstantsUtils.AlertStatusUtils.DUE:
                    contactTextView.setVisibility(View.GONE);
                    dueButton.setBackground(context.getResources().getDrawable(R.drawable.contact_due));
                    dueButton.setTextColor(context.getResources().getColor(R.color.vaccine_blue_bg_st));
                    break;
                case ConstantsUtils.AlertStatusUtils.OVERDUE:
                    contactTextView.setVisibility(View.GONE);
                    dueButton.setBackgroundColor(context.getResources().getColor(R.color.vaccine_red_bg_st));
                    dueButton.setTextColor(context.getResources().getColor(R.color.white));
                    break;
                case ConstantsUtils.AlertStatusUtils.NOT_DUE:
                    contactTextView.setVisibility(View.GONE);
                    dueButton.setBackground(context.getResources().getDrawable(R.drawable.contact_not_due));
                    dueButton.setTextColor(context.getResources().getColor(R.color.vaccine_blue_bg_st));
                    break;
                case ConstantsUtils.AlertStatusUtils.DELIVERY_DUE:
                    contactTextView.setVisibility(View.GONE);
                    dueButton.setBackground(context.getResources().getDrawable(R.drawable.contact_due));
                    dueButton.setTextColor(context.getResources().getColor(R.color.vaccine_blue_bg_st));
                    dueButton.setText(context.getString(R.string.due_delivery));
                    break;
                case ConstantsUtils.AlertStatusUtils.EXPIRED:
                    contactTextView.setVisibility(View.GONE);
                    dueButton.setBackgroundColor(context.getResources().getColor(R.color.vaccine_red_bg_st));
                    dueButton.setTextColor(context.getResources().getColor(R.color.white));
                    dueButton.setText(context.getString(R.string.due_delivery));
                    break;
                case ConstantsUtils.AlertStatusUtils.TODAY:
                    dueButton.setVisibility(View.GONE);
                    contactTextView.setVisibility(View.VISIBLE);
                    contactTextView.setText(String.format(context.getString(R.string.contact_recorded_today),
                            Utils.getTodayContact(String.valueOf(buttonAlertStatus.nextContact))));
                    contactTextView.setPadding(2, 2, 2, 2);

                    dueButton.setBackground(context.getResources().getDrawable(R.drawable.contact_disabled));
                    dueButton.setBackground(context.getResources().getDrawable(R.drawable.contact_disabled));
                    dueButton.setTextColor(context.getResources().getColor(R.color.dark_grey));
                    dueButton.setText(String.format(context.getString(R.string.contact_recorded_today_no_break),
                            Utils.getTodayContact(String.valueOf(buttonAlertStatus.nextContact))));
                    break;
                default:
                    dueButton.setBackground(context.getResources().getDrawable(R.drawable.contact_due));
                    dueButton.setTextColor(context.getResources().getColor(R.color.vaccine_blue_bg_st));
                    contactTextView.setVisibility(View.GONE);
                    break;
            }
        }
    }

    public static Integer getTodayContact(String nextContact) {
        Integer todayContact = 1;
        try {
            todayContact = Integer.valueOf(nextContact) - 1;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return todayContact;
    }

    /***
     * Save to shared preference
     * @param sharedPref name of shared preference file
     * @param key key to persist
     * @param value value to persist
     */
    public static void saveToSharedPreference(String sharedPref, String key, String value) {
        SharedPreferences.Editor editor = DrishtiApplication.getInstance().getSharedPreferences(
                sharedPref, Context.MODE_PRIVATE).edit();
        editor.putString(key, value).apply();
    }

    /***
     * Save to shared preference
     * @param sharedPref name of shared preference file
     * @param key key used to retrieve the value
     */
    public static String readFromSharedPreference(String sharedPref, String key) {
        SharedPreferences sharedPreferences = DrishtiApplication.getInstance().getSharedPreferences(
                sharedPref, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    /**
     * Checks if a table exists on the table. An {@link Exception} is expected to be thrown by the sqlite
     * database in case of anything weird such as the query being wrongly executed. This method is used
     * to determine critical operations such as migrations that if not done will case data corruption.
     * It is therefore safe to let the app crash instead of handling the error. So that the developer/user
     * can fix the issue before continuing with any other operations.
     *
     * @param sqliteDatabase
     * @param tableName
     * @return
     */
    public static boolean isTableExists(@NonNull SQLiteDatabase sqliteDatabase, @NonNull String tableName) {
        Cursor cursor = sqliteDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'",
                null
        );

        int nameColumnIndex = cursor.getColumnIndexOrThrow("name");
        while (cursor.moveToNext()) {
            String name = cursor.getString(nameColumnIndex);

            if (name.equals(tableName)) {
                if (cursor != null) {
                    cursor.close();
                }

                return true;
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return false;
    }
}
