package com.example.android.inventoryprojectapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


import com.example.android.inventoryprojectapp.data.InventoryContract;

/**
 * Displays list of products that were entered and stored in the app.
 */
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private InventoryCursorAdapter mInventoryCursorAdapter;
    private static final int URI_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find reference to the ListView
        initializeListView();

        // Start LoaderManager
        getLoaderManager().initLoader(URI_LOADER, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case URI_LOADER:
                // Defining projection for the Cursor so that it contains all rows from the table
                String projection[] = {
                        InventoryContract.InventoryEntry._ID,
                        InventoryContract.InventoryEntry.COLUMN_Product_NAME,
                        InventoryContract.InventoryEntry.COLUMN_Product_PRICE,
                        InventoryContract.InventoryEntry.COLUMN_Product_QUANTITY,
                        InventoryContract.InventoryEntry.COLUMN_Product_IMAGE,
                };
                // Define sort order
                String sortOrder =
                        InventoryContract.InventoryEntry._ID + " DESC ";
                // Return cursor loader
                return new CursorLoader(
                        this,
                        InventoryContract.InventoryEntry.CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        try {
            mInventoryCursorAdapter.swapCursor(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mInventoryCursorAdapter.swapCursor(null);
    }

    private void initializeListView() {
        // Find the ListView
        ListView listView = (ListView) findViewById(R.id.list_view);
        // Define empty view so a specific layout can be displayed when
        // there is no data to be shown in the UI
        View emptyView = findViewById(R.id.empty_state_view);
        // Attach the empty view to the list view when there is no data to be shown
        listView.setEmptyView(emptyView);
        // Initialize the Cursor Adapter
        mInventoryCursorAdapter = new InventoryCursorAdapter(this, null, false);
        // Attach the adapter to the list view
        listView.setAdapter(mInventoryCursorAdapter);
        // Set Click Listener to the listView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.setData(ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu from the res/menu/menu_main_activity.xml file
        // This adds the given menu to the app bar
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a me nu option in the app bar overflow menu
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

