package org.smartregister.anc.library.model;

import android.text.TextUtils;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.rules.RuleConstant;

import org.jeasy.rules.api.Facts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.anc.library.AncLibrary;
import org.smartregister.anc.library.domain.WomanDetail;
import org.smartregister.anc.library.domain.YamlConfig;
import org.smartregister.anc.library.domain.YamlConfigItem;
import org.smartregister.anc.library.repository.PartialContactRepositoryHelper;
import org.smartregister.anc.library.repository.PatientRepositoryHelper;
import org.smartregister.anc.library.repository.PreviousContactRepositoryHelper;
import org.smartregister.anc.library.util.ConstantsUtils;
import org.smartregister.anc.library.util.ContactJsonFormUtils;
import org.smartregister.anc.library.util.DBConstantsUtils;
import org.smartregister.anc.library.util.FilePathUtils;
import org.smartregister.clientandeventmodel.Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.smartregister.anc.library.util.ContactJsonFormUtils.extractItemValue;

public class ContactVisit {
    private Map<String, String> details;
    private String referral;
    private String baseEntityId;
    private int nextContact;
    private String nextContactVisitDate;
    private PartialContactRepositoryHelper partialContactRepositoryHelper;
    private List<PartialContact> partialContactList;
    private Facts facts;
    private List<String> formSubmissionIDs;
    private WomanDetail womanDetail;
    private Map<String, Integer> attentionFlagCountMap = new HashMap<>();
    private List<String> parsableFormsList =
            Arrays.asList(ConstantsUtils.JsonFormUtils.ANC_QUICK_CHECK, ConstantsUtils.JsonFormUtils.ANC_PROFILE,
                    ConstantsUtils.JsonFormUtils.ANC_SYMPTOMS_FOLLOW_UP, ConstantsUtils.JsonFormUtils.ANC_PHYSICAL_EXAM,
                    ConstantsUtils.JsonFormUtils.ANC_TEST, ConstantsUtils.JsonFormUtils.ANC_COUNSELLING_TREATMENT);

    public ContactVisit(Map<String, String> details, String referral, String baseEntityId, int nextContact,
                        String nextContactVisitDate, PartialContactRepositoryHelper partialContactRepositoryHelper,
                        List<PartialContact> partialContactList) {
        this.details = details;
        this.referral = referral;
        this.baseEntityId = baseEntityId;
        this.nextContact = nextContact;
        this.nextContactVisitDate = nextContactVisitDate;
        this.partialContactRepositoryHelper = partialContactRepositoryHelper;
        this.partialContactList = partialContactList;
    }

    public Facts getFacts() {
        return facts;
    }

    public List<String> getFormSubmissionIDs() {
        return formSubmissionIDs;
    }

    public WomanDetail getWomanDetail() {
        return womanDetail;
    }

    public ContactVisit invoke() throws Exception {
        facts = new Facts();
        formSubmissionIDs = new ArrayList<>();

        updateEventAndRequiredStepsField(baseEntityId, partialContactRepositoryHelper, partialContactList, facts,
                formSubmissionIDs);

        womanDetail = getWomanDetail(baseEntityId, nextContactVisitDate, nextContact);

        processAttentionFlags(womanDetail, facts);

        if (referral != null) {
            int yellowFlagCount = 0;
            int redFlagCount = 0;
            if (details.containsKey(DBConstantsUtils.KeyUtils.YELLOW_FLAG_COUNT) && details.get(DBConstantsUtils.KeyUtils.YELLOW_FLAG_COUNT) != null) {
                yellowFlagCount = Integer.valueOf(details.get(DBConstantsUtils.KeyUtils.YELLOW_FLAG_COUNT));
            }

            if (details.containsKey(DBConstantsUtils.KeyUtils.RED_FLAG_COUNT) && details.get(DBConstantsUtils.KeyUtils.RED_FLAG_COUNT) != null) {
                redFlagCount = Integer.valueOf(details.get(DBConstantsUtils.KeyUtils.RED_FLAG_COUNT));
            }

            womanDetail.setYellowFlagCount(yellowFlagCount);
            womanDetail.setRedFlagCount(redFlagCount);
            womanDetail.setContactStatus(details.get(DBConstantsUtils.KeyUtils.CONTACT_STATUS));
            womanDetail.setReferral(true);
            womanDetail.setLastContactRecordDate(details.get(DBConstantsUtils.KeyUtils.LAST_CONTACT_RECORD_DATE));
        }
        PatientRepositoryHelper.updateContactVisitDetails(womanDetail, true);
        return this;
    }

    private void updateEventAndRequiredStepsField(String baseEntityId, PartialContactRepositoryHelper partialContactRepositoryHelper,
                                                  List<PartialContact> partialContactList, Facts facts,
                                                  List<String> formSubmissionIDs) throws Exception {
        if (partialContactList != null) {

            Collections.sort(partialContactList, new Comparator<PartialContact>() {
                @Override
                public int compare(PartialContact o1, PartialContact o2) {
                    return o1.getSortOrder().compareTo(o2.getSortOrder());
                }
            });

            for (PartialContact partialContact : partialContactList) {
                JSONObject formObject = org.smartregister.anc.library.util.JsonFormUtils.toJSONObject(
                        partialContact.getFormJsonDraft() != null ? partialContact.getFormJsonDraft() :
                                partialContact.getFormJson());

                if (formObject != null) {
                    //process form details
                    if (parsableFormsList.contains(partialContact.getType())) {
                        processFormFieldKeyValues(baseEntityId, formObject,
                                String.valueOf(partialContact.getContactNo()));
                    }

                    //process attention flags
                    ContactJsonFormUtils.processRequiredStepsField(facts, formObject);

                    //process events
                    Event event = org.smartregister.anc.library.util.JsonFormUtils.processContactFormEvent(formObject, baseEntityId);
                    formSubmissionIDs.add(event.getFormSubmissionId());

                    JSONObject eventJson = new JSONObject(org.smartregister.anc.library.util.JsonFormUtils.gson.toJson(event));
                    AncLibrary.getInstance().getEcSyncHelper().addEvent(baseEntityId, eventJson);
                }

                //Remove partial contact
                partialContactRepositoryHelper.deletePartialContact(partialContact.getId());
            }
        }
    }

    private WomanDetail getWomanDetail(String baseEntityId, String nextContactVisitDate, Integer nextContact) {
        WomanDetail womanDetail = new WomanDetail();
        womanDetail.setBaseEntityId(baseEntityId);
        womanDetail.setNextContact(nextContact);
        womanDetail.setNextContactDate(nextContactVisitDate);
        womanDetail.setContactStatus(ConstantsUtils.AlertStatusUtils.TODAY);
        return womanDetail;
    }

    private void processAttentionFlags(WomanDetail patientDetail, Facts facts) throws IOException {
        Iterable<Object> ruleObjects = AncLibrary.getInstance().readYaml(FilePathUtils.FileUtils.ATTENTION_FLAGS);

        for (Object ruleObject : ruleObjects) {
            YamlConfig attentionFlagConfig = (YamlConfig) ruleObject;

            for (YamlConfigItem yamlConfigItem : attentionFlagConfig.getFields()) {

                if (AncLibrary.getInstance().getAncRulesEngineHelper().getRelevance(facts, yamlConfigItem.getRelevance())) {
                    Integer requiredFieldCount = attentionFlagCountMap.get(attentionFlagConfig.getGroup());
                    requiredFieldCount = requiredFieldCount == null ? 1 : ++requiredFieldCount;
                    attentionFlagCountMap.put(attentionFlagConfig.getGroup(), requiredFieldCount);

                }
            }
        }

        Integer redCount = attentionFlagCountMap.get(ConstantsUtils.AttentionFlagUtils.RED);
        Integer yellowCount = attentionFlagCountMap.get(ConstantsUtils.AttentionFlagUtils.YELLOW);
        patientDetail.setRedFlagCount(redCount != null ? redCount : 0);
        patientDetail.setYellowFlagCount(yellowCount != null ? yellowCount : 0);
    }

    private void processFormFieldKeyValues(String baseEntityId, JSONObject object, String contactNo) throws Exception {
        if (object != null) {
            persistRequiredInvisibleFields(baseEntityId, contactNo, object);
            Iterator<String> keys = object.keys();

            while (keys.hasNext()) {
                String key = keys.next();

                if (key.startsWith(RuleConstant.STEP)) {
                    JSONArray stepArray = object.getJSONObject(key).getJSONArray(JsonFormConstants.FIELDS);

                    for (int i = 0; i < stepArray.length(); i++) {
                        JSONObject fieldObject = stepArray.getJSONObject(i);
                        ContactJsonFormUtils.processSpecialWidgets(fieldObject);

                        if (fieldObject.getString(JsonFormConstants.TYPE).equals(JsonFormConstants.EXPANSION_PANEL)) {
                            saveExpansionPanelPreviousValues(baseEntityId, fieldObject, contactNo);
                            continue;
                        }

                        //Do not save empty checkbox values with nothing inside square braces ([])
                        if (fieldObject.has(JsonFormConstants.VALUE) &&
                                !TextUtils.isEmpty(fieldObject.getString(JsonFormConstants.VALUE)) &&
                                !isCheckboxValueEmpty(fieldObject)) {

                            fieldObject.put(PreviousContactRepositoryHelper.CONTACT_NO, contactNo);
                            savePreviousContactItem(baseEntityId, fieldObject);
                        }

                        if (fieldObject.has(ConstantsUtils.KeyUtils.SECONDARY_VALUES) &&
                                fieldObject.getJSONArray(ConstantsUtils.KeyUtils.SECONDARY_VALUES).length() > 0) {
                            JSONArray secondaryValues = fieldObject.getJSONArray(ConstantsUtils.KeyUtils.SECONDARY_VALUES);
                            for (int count = 0; count < secondaryValues.length(); count++) {
                                JSONObject secondaryValuesJSONObject = secondaryValues.getJSONObject(count);
                                secondaryValuesJSONObject.put(PreviousContactRepositoryHelper.CONTACT_NO, contactNo);
                                savePreviousContactItem(baseEntityId, secondaryValuesJSONObject);
                            }
                        }
                    }
                }
            }
        }

    }

    /***
     * Method that persist previous invisible required fields
     * @param baseEntityId unique Id for the woman
     * @param contactNo the contact number
     * @param object main form json object
     * @throws JSONException exception thrown
     */
    private void persistRequiredInvisibleFields(String baseEntityId, String contactNo, JSONObject object) throws JSONException {
        if (object.has(JsonFormConstants.INVISIBLE_REQUIRED_FIELDS)) {
            String key = JsonFormConstants.INVISIBLE_REQUIRED_FIELDS + "_" +
                    object.getString(ConstantsUtils.JsonFormKeyUtils.ENCOUNTER_TYPE)
                            .toLowerCase().replace(" ", "_");
            savePreviousContactItem(baseEntityId, new JSONObject()
                    .put(JsonFormConstants.KEY, key)
                    .put(JsonFormConstants.VALUE, object.getString(JsonFormConstants.INVISIBLE_REQUIRED_FIELDS))
                    .put(PreviousContactRepositoryHelper.CONTACT_NO, contactNo));

        }
    }

    private boolean isCheckboxValueEmpty(JSONObject fieldObject) throws JSONException {
        if (!fieldObject.has(JsonFormConstants.VALUE)) {
            return true;
        }
        String currentValue = fieldObject.getString(JsonFormConstants.VALUE);
        return TextUtils.equals(currentValue, "[]") || (currentValue.length() == 2
                && currentValue.startsWith("[") && currentValue.endsWith("]"));
    }

    private void saveExpansionPanelPreviousValues(String baseEntityId, JSONObject fieldObject, String contactNo)
            throws JSONException {
        if (fieldObject != null) {
            JSONArray value = fieldObject.optJSONArray(JsonFormConstants.VALUE);
            if (value == null) {
                return;
            }
            for (int j = 0; j < value.length(); j++) {
                JSONObject valueItem = value.getJSONObject(j);
                JSONArray valueItemJSONArray = valueItem.getJSONArray(JsonFormConstants.VALUES);
                String result = extractItemValue(valueItem, valueItemJSONArray);
                // do not save empty checkbox values ([])
                if (result.startsWith("[") && result.endsWith("]") && result.length() == 2 ||
                        TextUtils.equals("[]", result)) {
                    return;
                }
                JSONObject itemToSave = new JSONObject();
                itemToSave.put(JsonFormConstants.KEY, valueItem.getString(JsonFormConstants.KEY));
                itemToSave.put(JsonFormConstants.VALUE, result);
                itemToSave.put(PreviousContactRepositoryHelper.CONTACT_NO, contactNo);
                savePreviousContactItem(baseEntityId, itemToSave);
            }
        }
    }

    private void savePreviousContactItem(String baseEntityId, JSONObject fieldObject) throws JSONException {
        PreviousContact previousContact = new PreviousContact();
        previousContact.setKey(fieldObject.getString(JsonFormConstants.KEY));
        previousContact.setValue(fieldObject.getString(JsonFormConstants.VALUE));
        previousContact.setBaseEntityId(baseEntityId);
        previousContact.setContactNo(fieldObject.getString(PreviousContactRepositoryHelper.CONTACT_NO));
        getPreviousContactRepository().savePreviousContact(previousContact);
    }

    protected PreviousContactRepositoryHelper getPreviousContactRepository() {
        return AncLibrary.getInstance().getPreviousContactRepositoryHelper();
    }
}
