package com.example.android.inventoryprojectapp.data;

/**
 * Created by Polacek on 23.7.2017.
 */

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


public class InventoryContract {

    /**
     * CONTENT_AUTHORITY
     * In InventoryContract.java, we set this up as a string constant whose value is
     * the same as that from the AndroidManifest:
     * <p>
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.inventoryprojectapp";

    /**
     * BASE_CONTENT_URI
     * Next, we concatenate the CONTENT_AUTHORITY constant with the scheme “content://”
     * we will create the BASE_CONTENT_URI which will be shared by every URI
     * associated with InventoryContract
     * <p>
     * <p>
     * Using CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * PATH_TableName
     * This constants stores the path for each of the tables which
     * will be appended to the base content URI.
     * <p>
     * Possible path (appended to base content URI for possible URI's)
     * For instance, content://com.example.android.pets/pets/ is a valid path for
     * looking at pet data. content://com.example.android.pets/staff/ will fail,
     * as the ContentProvider hasn't been given any information on what to do with "staff".
     */
    public static final String PATH_PRODUCTS = "products";

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private InventoryContract() {
    }

    /**
     * Inner class that defines constant values for the products database table.
     * Each entry in the table represents a single product.
     */
    public static final class InventoryEntry implements BaseColumns {

        /**
         * Complete CONTENT_URI
         * Lastly, inside each of the Entry classes in the contract, we create a full URI
         * for the class as a constant called CONTENT_URI.
         * The Uri.withAppendedPath() method appends the BASE_CONTENT_URI
         * (which contains the scheme and the content authority) to the path segment.
         * <p>
         * The content URI to access the pet data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        // Table name
        public static final String TABLE_NAME = "products";

        // The _id field to index the table content
        public static final String _ID = BaseColumns._ID;

        // Product name
        public static final String COLUMN_Product_NAME = "name";

        // Product quantity
        public static final String COLUMN_Product_QUANTITY = "quantity";

        // Product price
        public static final String COLUMN_Product_PRICE = "price";

        // Product image
        public static final String COLUMN_Product_IMAGE = "image";
        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of products
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * The MIME type of the {@link #CONTENT_URI) for a single product
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

    }
}

