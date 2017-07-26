package com.example.android.inventoryprojectapp.data;

/**
 * Created by Polacek on 23.7.2017.
 */

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Martin on 17.7.2017 г..
 */

public class InventoryProvider extends ContentProvider {

    // Tag for the Log Messages
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();
    // Uri matcher code for the content URI for the products table
    private static final int PRODUCTS = 100;
    // Uri matcher code for the content URI for a single product in the products table
    private static final int PRODUCT_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.inventoryproject/products" will map to the
        // integer code {@link #PRODUCTS}. This URI is used to provide access to MULTIPLE rows
        // of the pets table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS, PRODUCTS);
        // The content URI of the form "content://com.example.android.inventoryproject/products/#" will map to the
        // integer code {@link #PRODUCT_ID}. This URI is used to provide access to ONE single row
        // of the pets table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.inventoryproject/products/3" matches, but
        // "content://com.example.android.pets/pets" (without a number at the end) doesn't match.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    // Database helper object
    private InventoryDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        // Create and initialize ProductDbHelper to gain access to the products database.
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        // This cursor will hold the result of the query
        Cursor cursor = null;
        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // For the PRODUCTS code we query multiple rows of the table directly with the
                // give projection, selection, selectionArgs, and sort order.
                cursor = database.query(InventoryContract.InventoryEntry.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        sortOrder);
                break;
            case PRODUCT_ID:
                // For the PRODUCT_ID code , we extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.inventoryproject/products/3",
                // the selection will be "_id=?" and the selection argument will be a String array
                // containing the actual ID of 3 in the case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(InventoryContract.InventoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
        }
        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        // Return the Cursor object
        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return InventoryContract.InventoryEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return InventoryContract.InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " With match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // Match the Uri to know in which table we're inserting new data
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
    }

    /**
     * Insert a product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {
        // Open a database to write into
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Insert into the database and return the new register ID
        long id = database.insert(InventoryContract.InventoryEntry.TABLE_NAME, null, values);
        // Return the Uri with the new Id to be used on the UI
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert new row for " + uri);
            return null;
        }
        // Set notification URI so the cursor can be updated automatically
        getContext().getContentResolver().notifyChange(uri, null);
        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Track the number of row/s that were deleted
        int rowDeleted;
        // Match the Uri to know which type of query is being done by using switch/case statement
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selectionArgs
                rowDeleted = database.delete(InventoryContract.InventoryEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowDeleted = database.delete(InventoryContract.InventoryEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        // I one or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowDeleted;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Set notification URI so the cursor can be updated automatically
                getContext().getContentResolver().notifyChange(uri, null);
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCT_ID:
                // Set notification URI so the cursor can be updated automatically
                getContext().getContentResolver().notifyChange(uri, null);
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not suppoted for " + uri);
        }
    }

    /**
     * Update product in the database with the given content values. Apply the changes to the row
     * specified in the selection and selection arguments.
     * Return the number of rows that were successfully updated.
     */
    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Open a database to write into
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // If there is no values to update, then do not bother
        if (values.size() == 0) {
            return 0;
        }

        // Update the selected product in the inventory database table with the given ContentValues
        int rowUpdated = database.update(InventoryContract.InventoryEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        // If one or more rows were updated, the notify all listener that the data at the given
        // URI has been changed
        if (rowUpdated != 0) {
            //Set notification URI so the cursor can be updated automatically
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of row/s that were updated
        return rowUpdated;
    }
}

