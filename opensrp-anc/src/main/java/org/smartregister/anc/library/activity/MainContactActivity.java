package org.smartregister.anc.library.activity;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.rules.RuleConstant;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.anc.library.AncLibrary;
import org.smartregister.anc.library.R;
import org.smartregister.anc.library.contract.ContactContract;
import org.smartregister.anc.library.domain.Contact;
import org.smartregister.anc.library.model.PartialContact;
import org.smartregister.anc.library.model.PreviousContact;
import org.smartregister.anc.library.presenter.ContactPresenter;
import org.smartregister.anc.library.util.ConstantsUtils;
import org.smartregister.anc.library.util.ContactJsonFormUtils;
import org.smartregister.anc.library.util.DBConstantsUtils;
import org.smartregister.anc.library.util.FilePathUtils;
import org.smartregister.anc.library.util.Utils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainContactActivity extends BaseContactActivity implements ContactContract.View {

    public static final String TAG = MainContactActivity.class.getCanonicalName();

    private TextView patientNameView;

    private Map<String, Integer> requiredFieldsMap = new HashMap<>();
    private Map<String, String> eventToFileMap = new HashMap<>();
    private Yaml yaml = new Yaml();
    private Map<String, List<String>> formGlobalKeys = new HashMap<>();
    private Map<String, String> formGlobalValues = new HashMap<>();
    private Set<String> globalKeys = new HashSet<>();
    private Set<String> defaultValueFields = new HashSet<>();
    private List<String> globalValueFields = new ArrayList<>();
    private List<String> editableFields = new ArrayList<>();
    private String baseEntityId;
    private String womanAge = "";
    private List<String> invisibleRequiredFields = new ArrayList<>();

    @Override
    protected void onResume() {
        super.onResume();

        baseEntityId = getIntent().getStringExtra(ConstantsUtils.IntentKeyUtils.BASE_ENTITY_ID);
        contactNo = getIntent().getIntExtra(ConstantsUtils.IntentKeyUtils.CONTACT_NO, 1);
        @SuppressWarnings("unchecked") Map<String, String> womanDetails =
                (Map<String, String>) getIntent().getSerializableExtra(ConstantsUtils.IntentKeyUtils.CLIENT_MAP);
        womanAge = String.valueOf(Utils.getAgeFromDate(womanDetails.get(DBConstantsUtils.KeyUtils.DOB)));

        if (!presenter.baseEntityIdExists()) {
            presenter.setBaseEntityId(baseEntityId);
        }

        initializeMainContactContainers();

        //Enable/Disable finalize button
        findViewById(R.id.finalize_contact).setEnabled(getRequiredCountTotal() == 0);
    }

    private void initializeMainContactContainers() {

        try {

            requiredFieldsMap.clear();

            loadContactGlobalsConfig();

            process(new String[]{ConstantsUtils.JsonFormUtils.ANC_QUICK_CHECK, ConstantsUtils.JsonFormUtils.ANC_PROFILE,
                    ConstantsUtils.JsonFormUtils.ANC_SYMPTOMS_FOLLOW_UP, ConstantsUtils.JsonFormUtils.ANC_PHYSICAL_EXAM,
                    ConstantsUtils.JsonFormUtils.ANC_TEST, ConstantsUtils.JsonFormUtils.ANC_COUNSELLING_TREATMENT});

            List<Contact> contacts = new ArrayList<>();

            Contact quickCheck = new Contact();
            quickCheck.setName(getString(R.string.quick_check));
            quickCheck.setContactNumber(contactNo);
            quickCheck.setActionBarBackground(R.color.quick_check_red);
            quickCheck.setBackground(R.drawable.quick_check_bg);
            quickCheck.setWizard(false);
            quickCheck.setHideSaveLabel(true);
            if (requiredFieldsMap.containsKey(quickCheck.getName())) {
                Integer quickCheckFields = requiredFieldsMap.get(quickCheck.getName());
                quickCheck.setRequiredFields(quickCheckFields != null ? quickCheckFields : 0);
            }

            quickCheck.setFormName(ConstantsUtils.JsonFormUtils.ANC_QUICK_CHECK);
            contacts.add(quickCheck);

            Contact profile = new Contact();
            profile.setName(getString(R.string.profile));
            profile.setContactNumber(contactNo);
            profile.setBackground(R.drawable.profile_bg);
            profile.setActionBarBackground(R.color.contact_profile_actionbar);
            profile.setNavigationBackground(R.color.contact_profile_navigation);
            if (requiredFieldsMap.containsKey(profile.getName())) {
                profile.setRequiredFields(requiredFieldsMap.get(profile.getName()));
            }
            profile.setFormName(ConstantsUtils.JsonFormUtils.ANC_PROFILE);
            contacts.add(profile);

            Contact symptomsAndFollowUp = new Contact();
            symptomsAndFollowUp.setName(getString(R.string.symptoms_follow_up));
            symptomsAndFollowUp.setContactNumber(contactNo);
            symptomsAndFollowUp.setBackground(R.drawable.symptoms_bg);
            symptomsAndFollowUp.setActionBarBackground(R.color.contact_symptoms_actionbar);
            symptomsAndFollowUp.setNavigationBackground(R.color.contact_symptoms_navigation);
            if (requiredFieldsMap.containsKey(symptomsAndFollowUp.getName())) {
                symptomsAndFollowUp.setRequiredFields(requiredFieldsMap.get(symptomsAndFollowUp.getName()));
            }
            symptomsAndFollowUp.setFormName(ConstantsUtils.JsonFormUtils.ANC_SYMPTOMS_FOLLOW_UP);
            contacts.add(symptomsAndFollowUp);

            Contact physicalExam = new Contact();
            physicalExam.setName(getString(R.string.physical_exam));
            physicalExam.setContactNumber(contactNo);
            physicalExam.setBackground(R.drawable.physical_exam_bg);
            physicalExam.setActionBarBackground(R.color.contact_exam_actionbar);
            physicalExam.setNavigationBackground(R.color.contact_exam_navigation);
            if (requiredFieldsMap.containsKey(physicalExam.getName())) {
                physicalExam.setRequiredFields(requiredFieldsMap.get(physicalExam.getName()));
            }
            physicalExam.setFormName(ConstantsUtils.JsonFormUtils.ANC_PHYSICAL_EXAM);
            contacts.add(physicalExam);

            Contact tests = new Contact();
            tests.setName(getString(R.string.tests));
            tests.setContactNumber(contactNo);
            tests.setBackground(R.drawable.tests_bg);
            tests.setActionBarBackground(R.color.contact_tests_actionbar);
            tests.setNavigationBackground(R.color.contact_tests_navigation);
            if (requiredFieldsMap.containsKey(tests.getName())) {
                tests.setRequiredFields(requiredFieldsMap.get(tests.getName()));
            }
            tests.setFormName(ConstantsUtils.JsonFormUtils.ANC_TEST);
            contacts.add(tests);

            Contact counsellingAndTreatment = new Contact();
            counsellingAndTreatment.setName(getString(R.string.counselling_treatment));
            counsellingAndTreatment.setContactNumber(contactNo);
            counsellingAndTreatment.setBackground(R.drawable.counselling_bg);
            counsellingAndTreatment.setActionBarBackground(R.color.contact_counselling_actionbar);
            counsellingAndTreatment.setNavigationBackground(R.color.contact_counselling_navigation);
            if (requiredFieldsMap.containsKey(counsellingAndTreatment.getName())) {
                counsellingAndTreatment.setRequiredFields(requiredFieldsMap.get(counsellingAndTreatment.getName()));
            }
            counsellingAndTreatment.setFormName(ConstantsUtils.JsonFormUtils.ANC_COUNSELLING_TREATMENT);
            contacts.add(counsellingAndTreatment);

            contactAdapter.setContacts(contacts);
            contactAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    private int getRequiredCountTotal() {
        int count = -1;
        Set<Map.Entry<String, Integer>> entries = requiredFieldsMap.entrySet();
        //We count required fields for all the 6 contact forms
        if (entries.size() == 6) {
            count++;
            for (Map.Entry<String, Integer> entry : entries) {
                count += entry.getValue();
            }
        }
        return count;
    }

    private void loadContactGlobalsConfig() throws IOException {
        Iterable<Object> contactGlobals = readYaml(FilePathUtils.FileUtils.CONTACT_GLOBALS);

        for (Object ruleObject : contactGlobals) {
            Map<String, Object> map = ((Map<String, Object>) ruleObject);

            formGlobalKeys.put(map.get(ConstantsUtils.FORM).toString(), (List<String>) map.get(JsonFormConstants.FIELDS));
            globalKeys.addAll((List<String>) map.get(JsonFormConstants.FIELDS));
        }
    }

    private void process(String[] mainContactForms) throws Exception {

        //Fetch and load previously saved values
        if (contactNo > 1) {
            for (String formEventType : new ArrayList<>(Arrays.asList(mainContactForms))) {
                if (eventToFileMap.containsValue(formEventType)) {
                    updateGlobalValuesWithDefaults(formEventType);
                }
            }
            //Get invisible required fields saved with the last contact visit
            for (String key : eventToFileMap.keySet()) {
                String prevKey = JsonFormConstants.INVISIBLE_REQUIRED_FIELDS + "_" +
                        key.toLowerCase().replace(" ", "_");
                String invisibleFields = getMapValue(prevKey);
                if (invisibleFields != null && invisibleFields.length() > 2) {
                    String toSplit = invisibleFields.substring(1, invisibleFields.length() - 1);
                    invisibleRequiredFields.addAll(Arrays.asList(toSplit.split(",")));
                }
            }
            //Make profile always complete on second contact onwards
            requiredFieldsMap.put(ConstantsUtils.JsonFormUtils.ANC_PROFILE_ENCOUNTER_TYPE, 0);

        }

        JSONObject object;
        List<String> partialForms = new ArrayList<>(Arrays.asList(mainContactForms));

        List<PartialContact> partialContacts = AncLibrary.getInstance().getPartialContactRepositoryHelper()
                .getPartialContacts(getIntent().getStringExtra(ConstantsUtils.IntentKeyUtils.BASE_ENTITY_ID), contactNo);

        for (PartialContact partialContact : partialContacts) {
            if (partialContact.getFormJsonDraft() != null || partialContact.getFormJson() != null) {
                object = new JSONObject(partialContact.getFormJsonDraft() != null ? partialContact.getFormJsonDraft() :
                        partialContact.getFormJson());
                processRequiredStepsField(object);
                if (object.has(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE)) {
                    partialForms.remove(eventToFileMap.get(object.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE)));
                }
            }
        }

    }

    public Iterable<Object> readYaml(String filename) throws IOException {
        InputStreamReader inputStreamReader =
                new InputStreamReader(this.getAssets().open((FilePathUtils.FolderUtils.CONFIG_FOLDER_PATH + filename)));
        return yaml.loadAll(inputStreamReader);
    }

    /**
     * Get default values and set global values based on these keys
     *
     * @param formEventType list of forms
     */
    @SuppressLint("StaticFieldLeak")
    private void updateGlobalValuesWithDefaults(final String formEventType) {
        JSONObject mainJson;
        try {
            mainJson = org.smartregister.anc.library.util.JsonFormUtils.readJsonFromAsset(getApplicationContext(), "json.form/" + formEventType);
            if (mainJson.has(ConstantsUtils.DEFAULT_VALUES)) {
                JSONArray defaultValuesArray = mainJson.getJSONArray(ConstantsUtils.DEFAULT_VALUES);
                defaultValueFields.addAll(getListValues(defaultValuesArray));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error converting file to JsonObject ", e);
        } catch (Exception e) {
            Log.e(TAG, "Error reading json from asset file ", e);
        }

    }

    private String getMapValue(String key) {

        PreviousContact request = new PreviousContact();
        request.setBaseEntityId(baseEntityId);
        request.setKey(key);
        if (contactNo > 1) {
            request.setContactNo(String.valueOf(contactNo - 1));
        }

        PreviousContact previousContact =
                AncLibrary.getInstance().getPreviousContactRepositoryHelper().getPreviousContact(request);

        return previousContact != null ? previousContact.getValue() : null;
    }

    private void processRequiredStepsField(JSONObject object) throws Exception {
        if (object != null && object.has(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE)) {
            //initialize required fields map
            String encounterType = object.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE);

            //Do not add defaults for test and CT contact containers unless they are opened
            if (!ConstantsUtils.JsonFormUtils.ANC_COUNSELLING_TREATMENT_ENCOUNTER_TYPE.equals(encounterType)
                    && !ConstantsUtils.JsonFormUtils.ANC_TEST_ENCOUNTER_TYPE.equals(encounterType) &&
                    (requiredFieldsMap.size() == 0 || !requiredFieldsMap.containsKey(encounterType))) {
                requiredFieldsMap.put(object.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE), 0);
            }
            if (contactNo > 1 && ConstantsUtils.JsonFormUtils.ANC_PROFILE_ENCOUNTER_TYPE.equals(encounterType)) {
                requiredFieldsMap.put(ConstantsUtils.JsonFormUtils.ANC_PROFILE_ENCOUNTER_TYPE, 0);
                return;
            }
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith(RuleConstant.STEP)) {
                    JSONArray stepArray = object.getJSONObject(key).getJSONArray(JsonFormConstants.FIELDS);
                    for (int i = 0; i < stepArray.length(); i++) {
                        JSONObject fieldObject = stepArray.getJSONObject(i);
                        ContactJsonFormUtils.processSpecialWidgets(fieldObject);
                        boolean isFieldVisible = !fieldObject.has(JsonFormConstants.IS_VISIBLE) ||
                                fieldObject.getBoolean(JsonFormConstants.IS_VISIBLE);
                        boolean isRequiredField = isFieldVisible && org.smartregister.anc.library.util.JsonFormUtils.isFieldRequired(fieldObject);
                        updateFieldRequiredCount(object, fieldObject, isRequiredField);
                        updateFormGlobalValues(fieldObject);
                        checkRequiredForSubForms(fieldObject, object);
                    }
                }
            }
        }
    }

    private List<String> getListValues(JSONArray jsonArray) {
        if (jsonArray != null) {
            return AncLibrary.getInstance().getGsonInstance()
                    .fromJson(jsonArray.toString(), new TypeToken<List<String>>() {
                    }.getType());
        } else {
            return new ArrayList<>();
        }
    }

    private void updateFieldRequiredCount(JSONObject object, JSONObject fieldObject, boolean isRequiredField) throws JSONException {
        if (isRequiredField && (!fieldObject.has(JsonFormConstants.VALUE) ||
                TextUtils.isEmpty(fieldObject.getString(JsonFormConstants.VALUE)))) {
            Integer requiredFieldCount = requiredFieldsMap.get(object.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE));
            requiredFieldCount = requiredFieldCount == null ? 1 : ++requiredFieldCount;
            requiredFieldsMap.put(object.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE), requiredFieldCount);
        }
    }

    private void updateFormGlobalValues(JSONObject fieldObject) throws Exception {

        if (globalKeys.contains(fieldObject.getString(JsonFormConstants.KEY)) &&
                fieldObject.has(JsonFormConstants.VALUE)) {

            formGlobalValues.put(fieldObject.getString(JsonFormConstants.KEY),
                    fieldObject.getString(JsonFormConstants.VALUE));//Normal value
            processAbnormalValues(formGlobalValues, fieldObject);

            String secKey = ContactJsonFormUtils.getSecondaryKey(fieldObject);
            if (fieldObject.has(secKey)) {
                formGlobalValues.put(secKey, fieldObject.getString(secKey));//Normal value secondary key
            }

            if (fieldObject.has(ConstantsUtils.KeyUtils.SECONDARY_VALUES)) {
                fieldObject.put(ConstantsUtils.KeyUtils.SECONDARY_VALUES,
                        ContactJsonFormUtils.sortSecondaryValues(fieldObject));//sort and reset
                JSONArray secondaryValues = fieldObject.getJSONArray(ConstantsUtils.KeyUtils.SECONDARY_VALUES);
                for (int j = 0; j < secondaryValues.length(); j++) {
                    JSONObject jsonObject = secondaryValues.getJSONObject(j);
                    processAbnormalValues(formGlobalValues, jsonObject);
                }
            }
            checkRequiredForCheckBoxOther(fieldObject);
        }
    }

    private void checkRequiredForSubForms(JSONObject fieldObject, JSONObject encounterObject) throws JSONException {
        if (fieldObject.has(JsonFormConstants.CONTENT_FORM)) {
            if ((fieldObject.has(JsonFormConstants.IS_VISIBLE) && !fieldObject.getBoolean(JsonFormConstants.IS_VISIBLE))) {
                return;
            }
            JSONArray requiredFieldsArray = fieldObject.has(ConstantsUtils.REQUIRED_FIELDS) ?
                    fieldObject.getJSONArray(ConstantsUtils.REQUIRED_FIELDS) : new JSONArray();
            updateSubFormRequiredCount(requiredFieldsArray, createAccordionValuesMap(fieldObject, encounterObject), encounterObject);
            updateFormGlobalValuesFromExpansionPanel(fieldObject);
        }
    }

    public static void processAbnormalValues(Map<String, String> facts, JSONObject jsonObject) throws Exception {

        String fieldKey = ContactJsonFormUtils.getObjectKey(jsonObject);
        Object fieldValue = ContactJsonFormUtils.getObjectValue(jsonObject);
        String fieldKeySecondary = fieldKey.contains(ConstantsUtils.SuffixUtils.OTHER) ?
                fieldKey.substring(0, fieldKey.indexOf(ConstantsUtils.SuffixUtils.OTHER)) + ConstantsUtils.SuffixUtils.VALUE : "";
        String fieldKeyOtherValue = fieldKey + ConstantsUtils.SuffixUtils.VALUE;

        if (fieldKey.endsWith(ConstantsUtils.SuffixUtils.OTHER) && !fieldKeySecondary.isEmpty() &&
                facts.get(fieldKeySecondary) != null && facts.get(fieldKeyOtherValue) != null) {

            List<String> tempList = new ArrayList<>(Arrays.asList(facts.get(fieldKeySecondary).split("\\s*,\\s*")));
            tempList.remove(tempList.size() - 1);
            tempList.add(StringUtils.capitalize(facts.get(fieldKeyOtherValue)));
            facts.put(fieldKeySecondary, ContactJsonFormUtils.getListValuesAsString(tempList));

        } else {
            facts.put(fieldKey, fieldValue.toString());
        }

    }

    private void checkRequiredForCheckBoxOther(JSONObject fieldObject) throws Exception {
        //Other field for check boxes
        if (fieldObject.has(JsonFormConstants.VALUE) && !TextUtils.isEmpty(fieldObject.getString(JsonFormConstants.VALUE)) &&
                fieldObject.getString(ConstantsUtils.KeyUtils.KEY).endsWith(ConstantsUtils.SuffixUtils.OTHER) && formGlobalValues
                .get(fieldObject.getString(ConstantsUtils.KeyUtils.KEY).replace(ConstantsUtils.SuffixUtils.OTHER, ConstantsUtils.SuffixUtils.VALUE)) !=
                null) {

            formGlobalValues
                    .put(ContactJsonFormUtils.getSecondaryKey(fieldObject), fieldObject.getString(JsonFormConstants.VALUE));
            processAbnormalValues(formGlobalValues, fieldObject);
        }
    }

    private void updateSubFormRequiredCount(JSONArray requiredAccordionFields, HashMap<String, JSONArray> accordionValuesMap,
                                            JSONObject encounterObject) throws JSONException {

        if (requiredAccordionFields.length() == 0) {
            return;
        }

        String encounterType = encounterObject.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE);
        Integer requiredFieldCount = requiredFieldsMap.get(encounterType);

        for (int count = 0; count < requiredAccordionFields.length(); count++) {
            String item = requiredAccordionFields.getString(count);
            if (!accordionValuesMap.containsKey(item)) {
                requiredFieldCount = requiredFieldCount == null ? 1 : ++requiredFieldCount;
            }
        }

        if (requiredFieldCount != null) {
            requiredFieldsMap.put(encounterType, requiredFieldCount);
        }
    }

    private HashMap<String, JSONArray> createAccordionValuesMap(JSONObject accordionJsonObject, JSONObject encounterObject)
            throws JSONException {
        HashMap<String, JSONArray> accordionValuesMap = new HashMap<>();
        if (accordionJsonObject.has(JsonFormConstants.VALUE)) {
            String encounterType = encounterObject.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE);
            //At this point we can now initialize required value for Test/CT since at least on of the expansion widget has a value
            if (requiredFieldsMap.size() == 0 || !requiredFieldsMap.containsKey(encounterType)) {
                requiredFieldsMap.put(encounterType, 0);
            }

            JSONArray accordionValues = accordionJsonObject.getJSONArray(JsonFormConstants.VALUE);
            for (int index = 0; index < accordionValues.length(); index++) {
                JSONObject item = accordionValues.getJSONObject(index);
                accordionValuesMap.put(item.getString(JsonFormConstants.KEY),
                        item.getJSONArray(JsonFormConstants.VALUES));
            }
            return accordionValuesMap;
        }
        return accordionValuesMap;
    }

    private void updateFormGlobalValuesFromExpansionPanel(JSONObject fieldObject) throws JSONException {
        if (fieldObject.has(JsonFormConstants.VALUE) && fieldObject.has(JsonFormConstants.TYPE)
                && TextUtils.equals(JsonFormConstants.EXPANSION_PANEL, fieldObject.getString(JsonFormConstants.TYPE))) {

            JSONArray accordionValue = fieldObject.getJSONArray(JsonFormConstants.VALUE);
            for (int count = 0; count < accordionValue.length(); count++) {
                JSONObject item = accordionValue.getJSONObject(count);
                JSONArray itemValueJSONArray = item.optJSONArray(JsonFormConstants.VALUES);
                if (itemValueJSONArray != null) {
                    String itemValue = ContactJsonFormUtils.extractItemValue(item, itemValueJSONArray);
                    if (globalKeys.contains(item.getString(JsonFormConstants.KEY))) {
                        formGlobalValues.put(item.getString(JsonFormConstants.KEY), itemValue);
                    }
                }
            }

        }
    }

    @Override
    protected void createContacts() {
        try {
            eventToFileMap.put(getString(R.string.quick_check), ConstantsUtils.JsonFormUtils.ANC_QUICK_CHECK);
            eventToFileMap.put(getString(R.string.profile), ConstantsUtils.JsonFormUtils.ANC_PROFILE);
            eventToFileMap.put(getString(R.string.physical_exam), ConstantsUtils.JsonFormUtils.ANC_PHYSICAL_EXAM);
            eventToFileMap.put(getString(R.string.tests), ConstantsUtils.JsonFormUtils.ANC_TEST);
            eventToFileMap.put(getString(R.string.counselling_treatment), ConstantsUtils.JsonFormUtils.ANC_COUNSELLING_TREATMENT);
            eventToFileMap.put(getString(R.string.symptoms_follow_up), ConstantsUtils.JsonFormUtils.ANC_SYMPTOMS_FOLLOW_UP);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void initializePresenter() {
        presenter = new ContactPresenter(this);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        patientNameView = findViewById(R.id.top_patient_name);
        AncLibrary.getInstance().populateGlobalSettings();
    }

    @Override
    public void startFormActivity(JSONObject form, Contact contact) {
        super.startFormActivity(form, contact);
    }

    @Override
    protected String getFormJson(PartialContact partialContactRequest, JSONObject form) {
        try {
            JSONObject object = ContactJsonFormUtils.getFormJsonCore(partialContactRequest, form);
            preProcessDefaultValues(object);
            return object.toString();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    private void preProcessDefaultValues(JSONObject object) {
        try {
            if (object != null) {
                Iterator<String> keys = object.keys();

                while (keys.hasNext()) {
                    String key = keys.next();

                    if (ConstantsUtils.GLOBAL_PREVIOUS.equals(key)) {

                        JSONArray globalPreviousValues = object.getJSONArray(key);
                        globalValueFields = getListValues(globalPreviousValues);

                    }

                    if (ConstantsUtils.EDITABLE_FIELDS.equals(key)) {

                        JSONArray editableFieldValues = object.getJSONArray(key);
                        editableFields = getListValues(editableFieldValues);

                    }

                    if (key.startsWith(RuleConstant.STEP)) {
                        JSONArray stepArray = object.getJSONObject(key).getJSONArray(JsonFormConstants.FIELDS);

                        for (int i = 0; i < stepArray.length(); i++) {
                            JSONObject fieldObject = stepArray.getJSONObject(i);
                            updateDefaultValues(stepArray, i, fieldObject);
                        }
                    }
                }
                getValueMap(object);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void updateDefaultValues(JSONArray stepArray, int i, JSONObject fieldObject) throws JSONException {
        if (defaultValueFields.contains(fieldObject.getString(JsonFormConstants.KEY))) {

            if (!fieldObject.has(JsonFormConstants.VALUE) ||
                    TextUtils.isEmpty(fieldObject.getString(JsonFormConstants.VALUE))) {

                String defaultKey = fieldObject.getString(JsonFormConstants.KEY);
                String mapValue = getMapValue(defaultKey);

                if (mapValue != null) {
                    fieldObject.put(JsonFormConstants.VALUE, mapValue);
                    fieldObject.put(JsonFormConstants.EDITABLE, editableFields.contains(defaultKey));
                    fieldObject.put(JsonFormConstants.READ_ONLY, editableFields.contains(defaultKey));
                }

            }

            if (fieldObject.has(JsonFormConstants.OPTIONS_FIELD_NAME)) {
                boolean addDefaults = true;

                for (int m = 0; m < fieldObject.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).length(); m++) {
                    String optionValue;
                    if (fieldObject.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(m)
                            .has(JsonFormConstants.VALUE)) {
                        optionValue = fieldObject.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(m)
                                .getString(JsonFormConstants.VALUE);
                        if (ConstantsUtils.BooleanUtils.TRUE.equals(optionValue)) {
                            addDefaults = false;
                            break;
                        }
                    }
                }

                if (addDefaults && fieldObject.getString(JsonFormConstants.TYPE).equals(JsonFormConstants.CHECK_BOX) &&
                        fieldObject.has(JsonFormConstants.VALUE)) {
                    List<String> values = Arrays.asList(fieldObject.getString(JsonFormConstants.VALUE)
                            .substring(1, fieldObject.getString(JsonFormConstants.VALUE).length() - 1).split(", "));

                    for (int m = 0; m < fieldObject.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).length(); m++) {

                        if (values.contains(fieldObject.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(m)
                                .getString(JsonFormConstants.KEY))) {
                            stepArray.getJSONObject(i).getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(m)
                                    .put(JsonFormConstants.VALUE, true);
                            fieldObject.put(JsonFormConstants.EDITABLE,
                                    editableFields.contains(fieldObject.getString(JsonFormConstants.KEY)));
                            fieldObject.put(JsonFormConstants.READ_ONLY,
                                    editableFields.contains(fieldObject.getString(JsonFormConstants.KEY)));
                        }

                    }
                }
            }
        }
    }

    private void getValueMap(JSONObject object) throws JSONException {
        initializeGlobalPreviousValues(object);
        for (int i = 0; i < globalValueFields.size(); i++) {
            String mapValue = getMapValue(globalValueFields.get(i));
            if (mapValue != null) {
                if (object.has(JsonFormConstants.JSON_FORM_KEY.GLOBAL)) {
                    object.getJSONObject(JsonFormConstants.JSON_FORM_KEY.GLOBAL)
                            .put(ConstantsUtils.PrefixUtils.PREVIOUS + globalValueFields.get(i), mapValue);
                } else {

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(ConstantsUtils.PrefixUtils.PREVIOUS + globalValueFields.get(i), mapValue);
                    object.put(JsonFormConstants.JSON_FORM_KEY.GLOBAL, jsonObject);
                }
            }

        }
    }

    /***
     * This method initializes all global_previous values with empty strings
     * @param object Form Json object
     * @throws JSONException
     */
    private void initializeGlobalPreviousValues(JSONObject object) throws JSONException {
        if (object.has(ConstantsUtils.GLOBAL_PREVIOUS)) {
            JSONArray globalPreviousArray = object.getJSONArray(ConstantsUtils.GLOBAL_PREVIOUS);
            for (int i = 0; i < globalPreviousArray.length(); i++) {
                if (object.has(JsonFormConstants.JSON_FORM_KEY.GLOBAL)) {
                    object.getJSONObject(JsonFormConstants.JSON_FORM_KEY.GLOBAL)
                            .put(ConstantsUtils.PrefixUtils.PREVIOUS + globalPreviousArray.getString(i), "");
                }
            }
        }
    }

    @Override
    public void displayPatientName(String patientName) {
        if (patientNameView != null && StringUtils.isNotBlank(patientName)) {
            patientNameView.setText(patientName);
        }
    }

    @Override
    public void displayToast(int resourceId) {
        Utils.showToast(getApplicationContext(), getString(resourceId));
    }

    @Override
    public void loadGlobals(Contact contact) {
        //Update global for fields that are default for contact greater than 1
        if (contactNo > 1) {
            for (String item : defaultValueFields) {
                if (globalKeys.contains(item)) {
                    formGlobalValues.put(item, getMapValue(item));
                }
            }
        }

        List<String> contactGlobals = formGlobalKeys.get(contact.getFormName());

        if (contactGlobals != null) {
            Map<String, String> map = new HashMap<>();
            for (String cg : contactGlobals) {
                if (formGlobalValues.containsKey(cg)) {
                    String some = map.get(cg);

                    if (some == null || !some.equals(formGlobalValues.get(cg))) {
                        map.put(cg, formGlobalValues.get(cg));
                    }

                } else {
                    map.put(cg, "");
                }
            }

            //Inject some form defaults from client details
            map.put(ConstantsUtils.KeyUtils.CONTACT_NO, contactNo.toString());
            map.put(ConstantsUtils.PREVIOUS_CONTACT_NO, contactNo > 1 ? String.valueOf(contactNo - 1) : "0");
            map.put(ConstantsUtils.AGE, womanAge);

            //Inject gestational age when it has not been calculated from profile form
            if (TextUtils.isEmpty(formGlobalValues.get(ConstantsUtils.GEST_AGE_OPENMRS))) {
                map.put(ConstantsUtils.GEST_AGE_OPENMRS, String.valueOf(presenter.getGestationAge()));
            }

            String lastContactDate =
                    ((HashMap<String, String>) getIntent().getSerializableExtra(ConstantsUtils.IntentKeyUtils.CLIENT_MAP))
                            .get(DBConstantsUtils.KeyUtils.LAST_CONTACT_RECORD_DATE);
            map.put(ConstantsUtils.KeyUtils.LAST_CONTACT_DATE,
                    !TextUtils.isEmpty(lastContactDate) ? Utils.reverseHyphenSeperatedValues(lastContactDate, "-") : "");

            contact.setGlobals(map);
        }

    }

    @Override
    protected void onCreation() {//Overriden
    }

    @Override
    protected void onResumption() {//Overridden from Secured Activity

    }

    @Override
    protected void onPause() {
        super.onPause();
        formGlobalValues.clear();
        invisibleRequiredFields.clear();
    }
}
