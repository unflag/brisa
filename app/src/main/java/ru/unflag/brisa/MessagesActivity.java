package ru.unflag.brisa;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import com.google.firebase.iid.FirebaseInstanceId;

public class MessagesActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemLongClickListener {

    public static final String LOG_TAG = "LOG_TAG";
    public static final String BROADCAST_ACTION = "com.google.firebase.MESSAGING_EVENT";
    public static final String MESSAGE_TAG = "MESSAGE";
    public static Resources resources;
    public static String filter;
    public static int minLines;


    TextView textViewTest;
    ListView listView;
    Toolbar toolbar;
    BroadcastReceiver messagesReceiver;
    MessagesAdapter messagesAdapter;
    DatabaseController databaseController;
    ActionBar actionBar;
    ActionBarDrawerToggle drawerToggle;
    DrawerLayout drawerLayout;
    SearchView searchView;
    NavigationView navigationView;
    LoaderManager loaderManager;
    AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        // init all
        resources = getResources();

        minLines = resources.getInteger(R.integer.MAX_LINES_DEFAULT);
        messagesAdapter= new MessagesAdapter(this, null, 0);

        listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(messagesAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setEmptyView(findViewById(R.id.empty));

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        actionBar = getActionBar();

        getWindow().clearFlags(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        navigationView = (NavigationView) findViewById(R.id.drawer_navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        databaseController = new DatabaseController(this);
        databaseController.open();
        loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);

        String authToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(LOG_TAG, "Current token: " + authToken);

        // testing incoming fcm message handling
        messagesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(MESSAGE_TAG);
                textViewTest.setText(message);
            }
        };

        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(messagesReceiver, intentFilter);

        // setting up drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_access, R.string.drawer_access) {

                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    invalidateOptionsMenu();
                }

                public void onDrawerOpened(View view) {
                    super.onDrawerOpened(view);
                    invalidateOptionsMenu();
                }
            };
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerLayout.addDrawerListener(drawerToggle);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        long msgId = viewHolder.msgId;
        int maxLines = viewHolder.textViewText.getLineCount();

        if (viewHolder.textViewText.getMaxLines() == minLines) {
            messagesAdapter.setExpanded(msgId, true);
            viewHolder.textViewText.setMaxLines(maxLines);
            viewHolder.expandIcon.setImageResource(R.drawable.ic_expand_less_black_24dp);
            if (viewHolder.msgStatus == 0) {
                parent.setBackgroundColor(Color.TRANSPARENT);
                databaseController.setRead(msgId);
                loaderManager.getLoader(0).onContentChanged();
            }
        }
        else {
            messagesAdapter.setExpanded(msgId, false);
            viewHolder.textViewText.setMaxLines(minLines);
            viewHolder.expandIcon.setImageResource(R.drawable.ic_expand_more_black_24dp);
        }
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        final long msgId = viewHolder.msgId;
        Log.d(LOG_TAG, "Message Id " + msgId);
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete message?");
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                databaseController.deleteMessage(msgId);
                loaderManager.getLoader(0).onContentChanged();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing
            }
        });
        builder.create().show();
        return true;
    }

    // group of loader callbacks
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new MessagesCursorLoader(this, databaseController);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        messagesAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    // group of menu callbacks - drawer and toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.toolbar, menu);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();

        // filter listview when string in searchview has 3 chars or more or show default data
        SearchView.OnQueryTextListener onQueryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String string) {
                if (string.length() >= 3) {
                    filter = string;
                    loaderManager.restartLoader(0, null, MessagesActivity.this);

                }
                else if (string.length() < 3) {
                    filter = null;
                    loaderManager.restartLoader(0, null, MessagesActivity.this);
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String string) {
                return true;
            }
        };
        searchView.setOnQueryTextListener(onQueryTextListener);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        drawerToggle.onConfigurationChanged(configuration);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (drawerToggle.onOptionsItemSelected(menuItem)) {
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.vibration) {
            Switch switchButton = (Switch) menuItem.getActionView().findViewById(R.id.switch_button);
            if (switchButton.isChecked()) {
                switchButton.toggle();
            } else {
                switchButton.toggle();
            }
        } else {
            DrawerItemDialog itemDialog = new DrawerItemDialog(this, databaseController, loaderManager, menuItem);
            itemDialog.getDialog().show();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        databaseController.close();
    }

    static class MessagesCursorLoader extends CursorLoader {
        DatabaseController databaseController;

        MessagesCursorLoader(Context context, DatabaseController databaseController) {
            super(context);
            this.databaseController = databaseController;
        }

        @Override
        public Cursor loadInBackground() {
            if (filter == null) {
                return databaseController.getMessages();
            }
            else {
                return databaseController.getFilteredMessages(filter);
            }
        }
    }

    // ViewHolder - static class which holds initialized views and some useful fields
    static class ViewHolder {
        long msgId;
        int msgStatus;
        TextView textViewSubject;
        TextView textViewText;
        TextView textViewLevel;
        TextView textViewTimestamp;
        ImageView expandIcon;
    }
}