package ru.unflag.brisa;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

class DrawerItemDialog {

    private AlertDialog.Builder builder;
    private MenuItem menuItem;
    private Context context;
    private DatabaseController databaseController;
    private LoaderManager loaderManager;

    DrawerItemDialog(Context context, DatabaseController databaseController, LoaderManager loaderManager, MenuItem menuItem) {
        this.builder = new AlertDialog.Builder(context);
        this.builder.setTitle(menuItem.getTitle());
        this.builder.setCancelable(true);
        this.menuItem = menuItem;
        this.context = context;
        this.databaseController = databaseController;
        this.loaderManager = loaderManager;
    }

    AlertDialog getDialog() {
        switch (menuItem.getItemId()) {
            case R.id.retention:
                final NumberPicker numberPicker = new NumberPicker(context);
                final FrameLayout parent = new FrameLayout(context);
                numberPicker.setMinValue(1);
                numberPicker.setMaxValue(999);
                numberPicker.setValue(30);
                parent.addView(numberPicker, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));
                builder.setView(parent);
                builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing
                    }
                });
                builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing
                    }
                });
                break;
            case R.id.set_all_read:
                builder.setMessage(R.string.drawer_item_agreement);
                builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        databaseController.setAllRead();
                        loaderManager.getLoader(0).onContentChanged();

                    }
                });
                builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing
                    }
                });
                break;
            case R.id.delete_all:
                builder.setMessage(R.string.drawer_item_agreement);
                builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        databaseController.deleteAllMessages();
                        loaderManager.getLoader(0).onContentChanged();
                    }
                });
                builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing
                    }
                });
                break;
            case R.id.about:
                builder.setMessage(R.string.about_brisa_message);
                builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing
                    }
                });
                break;
        }
        return builder.create();
    }
}
