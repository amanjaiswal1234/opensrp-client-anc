package org.smartregister.anc.repository;

import android.content.ContentValues;
import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.api.Facts;
import org.smartregister.anc.model.PreviousContact;
import org.smartregister.anc.util.Constants;
import org.smartregister.anc.util.ContactJsonFormUtils;
import org.smartregister.anc.util.Utils;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.Repository;

import java.util.ArrayList;
import java.util.List;

public class PreviousContactRepository extends BaseRepository {
    public static final String TABLE_NAME = "previous_contact";
    public static final String ID = "_id";
    public static final String BASE_ENTITY_ID = "base_entity_id";
    public static final String CONTACT_NO = "contact_no";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String CREATED_AT = "created_at";
    public static final String GEST_AGE = "gest_age";
    private static final String TAG = PreviousContactRepository.class.getCanonicalName();
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE " + TABLE_NAME + "(" + ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," + CONTACT_NO +
                    "  VARCHAR NOT NULL, " + BASE_ENTITY_ID + "  VARCHAR NOT NULL, " + KEY + "  VARCHAR, " + VALUE +
                    "  VARCHAR NOT NULL, " + CREATED_AT + " INTEGER NOT NULL)";

    private static final String INDEX_ID =
            "CREATE INDEX " + TABLE_NAME + "_" + ID + "_index ON " + TABLE_NAME + "(" + ID + " COLLATE NOCASE);";

    private String[] projectionArgs = new String[]{ID, CONTACT_NO, KEY, VALUE, BASE_ENTITY_ID, CREATED_AT};

    public PreviousContactRepository(Repository repository) {
        super(repository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_SQL);
        database.execSQL(INDEX_ID);
    }

    public void savePreviousContact(PreviousContact previousContact) {
        if (previousContact == null) return;
        previousContact.setVisitDate(Utils.getDBDateToday());
        getWritableDatabase().insert(TABLE_NAME, null, createValuesFor(previousContact));
    }

    private ContentValues createValuesFor(PreviousContact PreviousContact) {
        ContentValues values = new ContentValues();
        values.put(ID, PreviousContact.getId());
        values.put(CONTACT_NO, PreviousContact.getContactNo());
        values.put(BASE_ENTITY_ID, PreviousContact.getBaseEntityId());
        values.put(VALUE, PreviousContact.getValue());
        values.put(KEY, PreviousContact.getKey());
        values.put(CREATED_AT, PreviousContact.getVisitDate());
        return values;
    }

    /**
     * @param previousContactRequest object holding contact request params it MUST contain NON NULL values for key
     *                               baseEntityId contactNo
     */
    public PreviousContact getPreviousContact(PreviousContact previousContactRequest) {
        String selection = null;
        String[] selectionArgs = null;
        PreviousContact dbPreviousContact = null;
        Cursor mCursor = null;
        try {
            if (StringUtils.isNotBlank(previousContactRequest.getBaseEntityId()) &&
                    StringUtils.isNotBlank(previousContactRequest.getKey())) {
                selection = BASE_ENTITY_ID + " = ? " + COLLATE_NOCASE + " AND " + KEY + " = ? " + COLLATE_NOCASE;
                selectionArgs = new String[]{previousContactRequest.getBaseEntityId(), previousContactRequest.getKey()};
            }

            mCursor = getReadableDatabase()
                    .query(TABLE_NAME, projectionArgs, selection, selectionArgs, null, null, null, null);
            if (mCursor.getCount() > 0) {

                mCursor.moveToFirst();

                dbPreviousContact = getContactResult(mCursor);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return dbPreviousContact;
    }

    /**
     * @param baseEntityId is the Base entity Id No to filter by
     * @param keysList     an optional list of keys to query null otherwise to get all keys for that base entity id
     */
    public List<PreviousContact> getPreviousContacts(String baseEntityId, List<String> keysList) {
        Cursor mCursor = null;
        String selection = "";
        String[] selectionArgs = null;
        List<PreviousContact> previousContacts = new ArrayList<>();
        try {
            SQLiteDatabase db = getWritableDatabase();

            if (StringUtils.isNotBlank(baseEntityId)) {
                if (keysList != null) {

                    selection = BASE_ENTITY_ID + " = ? " + COLLATE_NOCASE + " AND " + KEY + " IN (?) " + COLLATE_NOCASE;
                    selectionArgs = new String[]{baseEntityId, ContactJsonFormUtils.getListValuesAsString(keysList)};

                } else {
                    selection = BASE_ENTITY_ID + " = ? " + COLLATE_NOCASE;
                    selectionArgs = new String[]{baseEntityId};
                }
            }

            mCursor = db.query(TABLE_NAME, projectionArgs, selection, selectionArgs, null, null, null, null);

            if (mCursor != null) {

                while (mCursor.moveToNext()) {

                    previousContacts.add(getContactResult(mCursor));

                }
                return previousContacts;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return previousContacts;
    }

    public Facts getPreviousContactsFacts(String baseEntityId) {
        Facts previousContactFacts = new Facts();
        Cursor previousContactsCursor = null;
        try {
            SQLiteDatabase database = getWritableDatabase();

            String selection = "";
            String orderBy = "order by abs_contact_no, contact_no,_id DESC";
            String[] selectionArgs = null;

            if (StringUtils.isNotBlank(baseEntityId)) {
                selection = "select *,  abs(" + CONTACT_NO + ") as abs_contact_no from " + TABLE_NAME + " where " +
                        BASE_ENTITY_ID + " = ? and (" + KEY + " = ? or " + KEY + " = ? or " + KEY + " = ?) " + orderBy;
                selectionArgs = new String[]{baseEntityId, Constants.ATTENTION_FLAG_FACTS, Constants.WEIGHT_GAIN,
                        Constants.PHYS_SYMPTOMS};
            }

            previousContactsCursor = database.rawQuery(selection, selectionArgs);

            if (previousContactsCursor != null) {
                while (previousContactsCursor.moveToNext()) {
                    String factKey = previousContactsCursor.getString(previousContactsCursor.getColumnIndex(KEY)) + ":" +
                            previousContactsCursor.getString(previousContactsCursor.getColumnIndex(CONTACT_NO)) + ":" +
                            previousContactsCursor.getString(previousContactsCursor.getColumnIndex(CREATED_AT));

                    previousContactFacts
                            .put(factKey, previousContactsCursor.getString(previousContactsCursor.getColumnIndex(VALUE)));

                }
            }

            return previousContactFacts;
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (previousContactsCursor != null) {
                previousContactsCursor.close();
            }
        }

        return previousContactFacts;
    }

    public Facts getPreviousContactTestsFacts(String baseEntityId) {
        Cursor mCursor = null;
        Facts previousContactsTestsFacts = new Facts();
        try {
            SQLiteDatabase db = getWritableDatabase();
            mCursor = getAllTests(baseEntityId, db);

            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    previousContactsTestsFacts.put(mCursor.getString(mCursor.getColumnIndex(KEY)),
                            mCursor.getString(mCursor.getColumnIndex(VALUE)));

                }
                return previousContactsTestsFacts;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return previousContactsTestsFacts;
    }

    /**
     * Gets all the tests recorded in all contacts for the specific patient
     *
     * @param baseEntityId
     * @param database
     * @return
     */
    private Cursor getAllTests(String baseEntityId, SQLiteDatabase database) {
        String selection = "";
        String orderBy = ID + " DESC";
        String[] selectionArgs = null;

        if (StringUtils.isNotBlank(baseEntityId)) {
            selection = BASE_ENTITY_ID + " = ?";
            selectionArgs = new String[]{baseEntityId};
        }

        return database.query(TABLE_NAME, projectionArgs, selection, selectionArgs, KEY, null, orderBy, null);
    }

    // todo uncomment when implementing the all test results feature
    /*private Cursor getAllTestResultsForIndividualTest(String baseEntityId, String indicator, SQLiteDatabase database) {
        String selection = "";
        String orderBy = ID + "DESC";
        String[] selectionArgs = null;

        if (StringUtils.isNoneEmpty(baseEntityId) && StringUtils.isNoneEmpty(indicator)) {
            selection = BASE_ENTITY_ID + " = ? And (" + KEY + " = ? OR " + KEY + " = " + GEST_AGE + ")";
            selectionArgs = new String[]{baseEntityId, indicator};
        }

        return database.query(TABLE_NAME, projectionArgs, selection, selectionArgs, null, null, orderBy, null);
    }*/

    public Facts getPreviousContactFacts(String baseEntityId, String contactNo) {
        Cursor mCursor = null;
        String selection = "";
        String orderBy = "created_at DESC";
        String[] selectionArgs = null;
        Facts previousContacts = new Facts();
        try {
            SQLiteDatabase db = getWritableDatabase();

            if (StringUtils.isNotBlank(baseEntityId) && StringUtils.isNotBlank(contactNo)) {
                selection = BASE_ENTITY_ID + " = ? AND " + CONTACT_NO + " = ?";
                selectionArgs = new String[]{baseEntityId, contactNo};
            }

            mCursor = db.query(TABLE_NAME, projectionArgs, selection, selectionArgs, null, null, orderBy, null);

            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    previousContacts.put(mCursor.getString(mCursor.getColumnIndex(KEY)),
                            mCursor.getString(mCursor.getColumnIndex(VALUE)));

                }
                return previousContacts;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return previousContacts;
    }

    public Facts getImmediatePreviousSchedule(String baseEntityId, String contactNo) {
        Cursor scheduleCursor = null;
        String selection = "";
        String orderBy = "created_at DESC";
        String[] selectionArgs = null;
        Facts schedule = new Facts();
        try {
            SQLiteDatabase db = getWritableDatabase();

            if (StringUtils.isNotBlank(baseEntityId) && StringUtils.isNotBlank(contactNo)) {
                selection = BASE_ENTITY_ID + " = ? AND " + CONTACT_NO + " = ?";
                selectionArgs = new String[]{baseEntityId, contactNo};
            }

            scheduleCursor = db.query(TABLE_NAME, projectionArgs, selection, selectionArgs, null, null, orderBy, null);

            if (scheduleCursor != null) {
                while (scheduleCursor.moveToNext()) {
                    if (Constants.CONTACT_SCHEDULE.equals(scheduleCursor.getString(scheduleCursor.getColumnIndex(KEY)))) {
                        schedule.put(scheduleCursor.getString(scheduleCursor.getColumnIndex(KEY)),
                                scheduleCursor.getString(scheduleCursor.getColumnIndex(VALUE)));
                    }
                }
                return schedule;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (scheduleCursor != null) {
                scheduleCursor.close();
            }
        }

        return schedule;
    }

    private PreviousContact getContactResult(Cursor cursor) {

        PreviousContact previousContact = new PreviousContact();
        previousContact.setId(cursor.getLong(cursor.getColumnIndex(ID)));
        previousContact.setKey(cursor.getString(cursor.getColumnIndex(KEY)));
        previousContact.setValue(cursor.getString(cursor.getColumnIndex(VALUE)));
        previousContact.setBaseEntityId(cursor.getString(cursor.getColumnIndex(BASE_ENTITY_ID)));
        previousContact.setVisitDate(cursor.getString(cursor.getColumnIndex(CREATED_AT)));

        return previousContact;
    }
}
