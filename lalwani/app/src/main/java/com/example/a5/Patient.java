package com.example.a5;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.sql.Statement;


public class Patient {
    private String mName;
    private int mAge;
    private int mID;
    private String mSex;
    private SQLiteDatabase mDB;
    private String mTableName;

    public Patient(String name, int ID, int age, String sex, String dbPath) {
        mName = name;
        mAge = age;
        mSex = sex;
        mID = ID;

        mTableName = mName+"_"+mID+"_"+mAge+"_"+mSex;

        try {
            mDB = SQLiteDatabase.openOrCreateDatabase(dbPath + "/lalwani.sqlite", null);
            mDB.beginTransaction();
            try {
                //perform your database operations here ...
                mDB.execSQL("create table "+ mTableName +" ("
                        + " timestamp int, "
                        + " x real, "
                        + " y real, "
                        + " z real); ");

                mDB.setTransactionSuccessful(); //commit your changes
            } catch (SQLiteException e) {
                //report problem
            } finally {
                mDB.endTransaction();
            }
        } catch (SQLException e) {
            Log.e("DB", e.getMessage());
        }
    }

    protected void finalize() {
        mDB.close();
    }

    public void addSamples(int timestamps[], float x[], float y[], float z[]) {
        mDB.beginTransaction();
        try {
            for(int i=0; i< timestamps.length; i++)
            //perform your database operations here ...
            mDB.execSQL("insert into "+ mTableName+" (timestamp, x, y, z) Values ("
                    + timestamps[i] + ", "
                    + x[i] + ", "
                    + y[i] + ", "
                    + z[i] + ")");

            mDB.setTransactionSuccessful(); //commit your changes
        } catch (SQLiteException e) {
            //report problem
            Log.e("DB", e.getMessage());
        } finally {
            mDB.endTransaction();
        }
    }

    public PatientData[] loadPatientData(){
        mDB.beginTransaction();
        PatientData patientData[] = null;

        SQLiteStatement statement = mDB.compileStatement("select * from "+ mTableName);
        Cursor cursor;
        cursor = mDB.rawQuery("select * from " + mTableName, null);

        try {
            cursor.moveToFirst();
            patientData = new PatientData[cursor.getCount()];

            for(int i=0 ;i< cursor.getCount(); i++) {
                patientData[i] = new PatientData();
                patientData[i].timestamp = cursor.getInt(0);
                patientData[i].x = cursor.getFloat(1);
                patientData[i].y = cursor.getFloat(2);
                patientData[i].z = cursor.getFloat(3);
                cursor.moveToNext();
            }
            return patientData;
        } catch (SQLiteException e) {
            //report problem
            Log.e("DB", e.getMessage());
        } finally {
            cursor.close();
        }
        return patientData;
    }

    public class PatientData{
        public int timestamp;
        public float x;
        public float y;
        public float z;
    }
}
