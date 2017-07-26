package com.example.android.inventoryprojectapp;

/**
 * Created by Polacek on 23.7.2017.
 */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryprojectapp.data.InventoryContract;

public class InventoryCursorAdapter extends CursorAdapter {

    Context mContext;

    public InventoryCursorAdapter(Context context, Cursor cursor, boolean autoReQuery) {
        super(context, cursor, autoReQuery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate and return a new view without binding any data
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method bind the product data (in the current row pointed by the cursor) to the given
     * list item layout
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find product name field, so it can be populated when inflated
        TextView productName = (TextView) view.findViewById(R.id.list_name_of_product);
        // Find product price field, so it can be populated when inflated
        TextView productPrice = (TextView) view.findViewById(R.id.list_price_of_product);
        // Find product quantity field, so it can be populated when inflated
        TextView productQuantity = (TextView) view.findViewById(R.id.list_quantity_of_product);
        ImageView productImage = (ImageView) view.findViewById(R.id.list_image);

        // Extract values from the Cursor object
        String name = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_Product_NAME));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_Product_PRICE));
        final int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_Product_QUANTITY));
        byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_Product_IMAGE));
        if (imageBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImage.setImageBitmap(bitmap);
        }
        final Uri uri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI,
                cursor.getInt(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry._ID)));

        // Populate the text views with values extracted from the Cursor object
        productName.setText(name);
        productPrice.setText(context.getString(R.string.eur_price) + " " + price);
        productQuantity.setText(quantity + " " + context.getString(R.string.quantity_avaiable));

        // Find sale button
        Button saleButton = (Button) view.findViewById(R.id.sale_item_list_button);
        // Set listener to the button
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if quantity in stock is higher than zero
                if (quantity > 0) {
                    // Assign a new quantity value of minus one to represent one item sold
                    int newQuantity = quantity - 1;
                    // Create and initialize a new Content Values object with the new quantity
                    ContentValues values = new ContentValues();
                    values.put(InventoryContract.InventoryEntry.COLUMN_Product_QUANTITY, newQuantity);
                    // Update the database
                    context.getContentResolver().update(uri, values, null, null);
                } else {
                    // Notify the user that quantity is less than zero and cannot be updated
                    Toast.makeText(context, context.getString(R.string.toast_product_not_avaiable),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
