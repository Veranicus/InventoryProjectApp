package com.example.android.inventoryprojectapp;

/**
 * Created by Polacek on 23.7.2017.
 */

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryprojectapp.data.InventoryContract;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Allows user to create a new pet or edit an existing one.
 */
public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Final for the image intent request code
    private final static int SELECT_Image = 10;
    // Constant to be used when asking for storage read
    private int mQuantityItem;
    private Button mSelectImageButton;
    // ImageView to display selected image
    private ImageView mItemImageView;
    // Bitmap to store/retrieve from the database
    private Bitmap mProductBitmap;
    // button to order more quantity from supplier
    private Button mOrderButton;
    // Uri received with the Intent from MainActivity
    private Uri mItemUri;
    private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 666;
    private static final int IMG_PIC = 1;
    // Constant field for email intent
    private static final String URI_EMAIL = "mailto:";
    // Uri loader
    private static final int URI_LOADER = 0;
    // EditText field to enter product name
    private EditText mNameEditText;
    // EditText field to enter product price
    private EditText mPriceEditText;
    // TextView to show the current product quantity
    private TextView mQuantityTextView;
    //Product information variables
    private String mProductName;
    // For modifying quantity
    private Button mIncreaseQuantityButton; // Increase by one
    private Button mDecreaseQuantityButton; // Decrease by one
    // Boolean to check whether or not the register has changed
    private boolean mProductChange = false;
    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mPetHasChanged boolean to true.
     */
    View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mProductChange = true;
            return false;
        }
    };

    /**
     * Helper method for converting from byte array to bitmap
     *
     * @param image BLOB from the database converted to a Bitmap
     *              in order to be displayed in the UI
     * @return
     */
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 70, image.length);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        //Receive Uri data from Intent
        Intent intent = getIntent();
        mItemUri = intent.getData();

        // Check if Uri is null or not
        if (mItemUri == null) {
            // If its null that means a new product
            setTitle(getString(R.string.add_new_item));
            // Invalidate options menu
            invalidateOptionsMenu();
        } else {
            // If its not null that means a product register will be edited
            setTitle(getString(R.string.edit_existing_item));
            // Kick off LoaderManager
            getLoaderManager().initLoader(URI_LOADER, null, this);
        }

        // Finding optimal views
        initialiseViews();

        // Setting onlicklistener to all views
        setOnTouchListener();
    }

    private void initialiseViews() {
        // Check if there is an existing product to make the button visible so
        // the user can order more from it
        if (mItemUri != null) {
            // Initialize button to order more from supplier
            mOrderButton = (Button) findViewById(R.id.Orderbutton);
            // Make button visible
            mOrderButton.setVisibility(View.VISIBLE);
            mOrderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setData(Uri.parse("mailto:"));
                    intent.setType("text/plain");
                    // Defining supplier's email.
                    intent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.email_supplier_edit));
                    intent.putExtra(Intent.EXTRA_SUBJECT, mProductName);
                    startActivity(Intent.createChooser(intent, "Send Mail..."));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });
        }

        // Edittext's initialization
        mNameEditText = (EditText) findViewById(R.id.detail_edit_name);
        mPriceEditText = (EditText) findViewById(R.id.price_edit_text);
        // Initialize TextView
        mQuantityTextView = (TextView) findViewById(R.id.quantity_text_view);

        // Increase button listener
        mIncreaseQuantityButton = (Button) findViewById(R.id.button_increase);
        mIncreaseQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add one to product
                mQuantityItem++;
                // Update UI
                mQuantityTextView.setText(String.valueOf(mQuantityItem));
            }
        });

        // Initialize decrease button and set listener to it
        mDecreaseQuantityButton = (Button) findViewById(R.id.button_decrease);
        mDecreaseQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the product quantity is higher thatn 0
                if (mQuantityItem > 0) {
                    // If its higher than 0 we can decrease by one
                        mQuantityItem--;
                    // Update the UI
                        mQuantityTextView.setText(String.valueOf(mQuantityItem));
                } else {
                    // If it's not higher than 0 notify the user
                    Toast.makeText(DetailActivity.this, getString(R.string.empty_quantity_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });

// Initialize the image view to show preview of the product image
        mItemImageView = (ImageView) findViewById(R.id.item_image);

        // Initialize button to select image for the product
        mSelectImageButton = (Button) findViewById(R.id.img_selec_button);
        mSelectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openImageSelector();

                // Ask for user permission to explore image gallery
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // If not authorized, ask for authorization
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                // Do something
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                        return;
                    }
                    // If permission granted, create a new intent and prompt
                    // user to pick image from Gallery
                    Intent getIntent = new Intent(Intent.ACTION_PICK);
                    getIntent.setType("image/*");
                    startActivityForResult(getIntent, SELECT_Image);
                }
            }
        });
    }

    // Handle the result of the image chooser intent launch
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if request code, result and intent match the image chooser
        if (requestCode == SELECT_Image && resultCode == RESULT_OK && data != null) {
            // Get image Uri
            Uri selectedImage = data.getData();
            Log.v("DetailActivity", "Uri: " + selectedImage.toString());
            // Get image file path
            String[] filePatchColumn = {MediaStore.Images.Media.DATA};
            // Create cursor object and query image
            Cursor cursor = getContentResolver().query(selectedImage, filePatchColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePatchColumn[0]);
            // Get image path from cursor
            String picturePath = cursor.getString(columnIndex);
            // Close cursor to avoid memory leaks
            cursor.close();
            // Set the image to a Bitmap object

            mProductBitmap = BitmapFactory.decodeFile(picturePath);

            mProductBitmap = getBitmapFromUri(selectedImage);
            // Set Bitmap to the image view
            mItemImageView = (ImageView) findViewById(R.id.item_image);
            mItemImageView.setImageBitmap(mProductBitmap);
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty()) {
            return null;
        }

        // Get the dimensions of the View
        mItemImageView = (ImageView) findViewById(R.id.item_image);
        int targetW = mItemImageView.getWidth();
        int targetH = mItemImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e("AddActivity", "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e("AddActivity", "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.editor_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item
        if (mItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.deleteMenuEdit);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Add" menu option
            case R.id.action_add:
                if (mProductChange) {
                    // Call save/edit method
                    saveProduct();
                } else {
                    // Show a toast message when no product is updated nor created
                    Toast.makeText(this, getString(R.string.toast_no_action), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.deleteMenuEdit:
                // Call delete confirmation dialog
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                // If product hasn't changed, continue with navigation up to parent activity
                if (!mProductChange) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                } else {
                    //Warning the user in a case of leaving with unfinished changes//
                    DialogInterface.OnClickListener discardButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // User clicked "Discard" button , navigate to parent activity
                                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                                }
                            };
                    // Show a dialog that notifies the user they have unsaved changes
                    showUnsavedChangesDialog(discardButtonClickListener);
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    // Back button logic
    @Override
    public void onBackPressed() {
        // If the product hasn't changed , continue with closing and back to parent activity
        if (!mProductChange) {
            super.onBackPressed();
        }

        // Conifrming the discard of changes
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close the current activity without adding/saving
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    // Add new product or commit changes to existing one
    private void saveProduct() {
        // Define whether or not EditText fields are empty
        boolean nameIsEmpty = checkFieldEmpty(mNameEditText.getText().toString().trim());
        boolean priceIsEmpty = checkFieldEmpty(mPriceEditText.getText().toString().trim());

        // If quantity, price or no image added
        if (nameIsEmpty) {
            Toast.makeText(this, getString(R.string.toast_invalid_name), Toast.LENGTH_SHORT).show();
        } else if (mQuantityItem <= 0) {
            Toast.makeText(this, getString(R.string.toast_invalid_quality_insert), Toast.LENGTH_SHORT).show();
        } else if (priceIsEmpty) {
            Toast.makeText(this, getString(R.string.toast_invalid_price_empty), Toast.LENGTH_SHORT).show();
        } else if (mProductBitmap == null) {
            Toast.makeText(this, getString(R.string.toast_no_image_added), Toast.LENGTH_SHORT).show();
        } else {
            // Assuming that all fields are valid, pass the name edit text
            // value to a String for easier manipulation
            String name = mNameEditText.getText().toString().trim();
            // Pass the price edit text value to a double for easier manipulation
            double price = Double.parseDouble(mPriceEditText.getText().toString().trim());

            // Create new Content Values and put the product info into them
            ContentValues values = new ContentValues();
            values.put(InventoryContract.InventoryEntry.COLUMN_Product_NAME, name);
                values.put(InventoryContract.InventoryEntry.COLUMN_Product_QUANTITY, mQuantityItem);
            values.put(InventoryContract.InventoryEntry.COLUMN_Product_PRICE, price);

            if (mProductBitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                boolean a = mProductBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                values.put(InventoryContract.InventoryEntry.COLUMN_Product_IMAGE, byteArray);
            }

            // Check if Uri is valid to determine whether is new product insertion or existing product update
            if (mItemUri == null) {
                // If Uri is null then we're inserting a new product
                Uri newUri = getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI, values);

                if (newUri == null) {
                    // Notify user for the successful product insertion
                    Toast.makeText(this, getString(R.string.toast_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the insertion was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.toast_succesfull),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                int rowsAffected = getContentResolver().update(mItemUri, values, null, null);

                // If the update was succesfull notify user
                if (rowsAffected == 0) {
                    // No affected errors means error occured
                    Toast.makeText(this, getString(R.string.toast_failed_update), Toast.LENGTH_SHORT).show();
                } else {
                    // In other case update was succesfull
                    Toast.makeText(this, getString(R.string.toast_update_succesfull),
                            Toast.LENGTH_SHORT).show();
                }

            }
            finish();
        }
    }

    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_Image);
    }

    /**
     * Helper method to define if any of the EditText fields are empty or contain invalid inputs
     */
    private boolean checkFieldEmpty(String string) {
        return TextUtils.isEmpty(string) || string.equals(".");
    }

    /**
     * Perform the deletion of the product in the database
     */
    private void deleteProduct() {
        if (mItemUri != null) {
            int rowsDeleted = getContentResolver().delete(mItemUri, null, null);
            // Notify user whether or not the delete was successful
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the deletion
                Toast.makeText(this, getString(R.string.toast_unsucessfull_delete), Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and notify the user
                Toast.makeText(this, getString(R.string.toast_succesfull_delete), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Aks for user confirmation before deleting product in database
     */
    private void showDeleteConfirmationDialog() {
        // Create a AlertDialog.Builder with confirmation message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.product_delete));
        // Set onClick Listeners for positive and negative options
        // Positive Option -> Yes! Delete!
        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Call deleteProduct method to delete the product
                deleteProduct();
                finish();
            }
        });
        // Negative option -> Cancel please
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Dismiss the dialog and continue editing the product
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Ask for user confirmation to exit activity before saving
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder with confirmation message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.leave_without_save));
        // Set onClick Listeners for positive and negative options
        // Positive Option -> Yes! Leave!
        builder.setPositiveButton(getString(R.string.yes), discardButtonClickListener);
        // Negative option -> Cancel! I want to stay
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Creating alert dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setOnTouchListener() {
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mIncreaseQuantityButton.setOnTouchListener(mTouchListener);
        mDecreaseQuantityButton.setOnTouchListener(mTouchListener);
        mSelectImageButton.setOnTouchListener(mTouchListener);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_Product_NAME,
                InventoryContract.InventoryEntry.COLUMN_Product_PRICE,
                InventoryContract.InventoryEntry.COLUMN_Product_QUANTITY,
                InventoryContract.InventoryEntry.COLUMN_Product_IMAGE
        };

        switch (id) {
            case URI_LOADER:
                return new CursorLoader(
                        this,
                        mItemUri,
                        projection,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }

        if (data.moveToFirst()) {
            mProductName = data.getString(data.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_Product_NAME));
            mNameEditText = (EditText) findViewById(R.id.detail_edit_name);
            mNameEditText.setText(mProductName);

            mPriceEditText = (EditText) findViewById(R.id.price_edit_text);
            mPriceEditText.setText(data.getString(data.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_Product_PRICE)));

            mQuantityTextView = (TextView) findViewById(R.id.quantity_text_view);
            mQuantityItem = data.getInt(data.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_Product_QUANTITY));
            mQuantityTextView.setText(String.valueOf(mQuantityItem));

            byte[] bytesArray = data.getBlob(data.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_Product_IMAGE));
            if (bytesArray != null) {
                mProductBitmap = BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.length);
                mItemImageView = (ImageView) findViewById(R.id.item_image);
                mItemImageView.setImageBitmap(mProductBitmap);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.getText().clear();
        mQuantityTextView.setText("");
    }
}
