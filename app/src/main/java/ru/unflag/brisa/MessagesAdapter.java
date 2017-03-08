package ru.unflag.brisa;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

// custom CursorAdapter which holds info about rows state and binds views to list
class MessagesAdapter extends CursorAdapter {

    private Resources resources = MessagesActivity.resources;
    private LongSparseArray<Boolean> expandedMap = new LongSparseArray<>();
    MessagesAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    void setExpanded(long id, boolean state) {
        expandedMap.put(id, state);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final MessagesActivity.ViewHolder viewHolder;
        View convertView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);

        viewHolder = new MessagesActivity.ViewHolder();
        viewHolder.textViewSubject = (TextView) convertView.findViewById(R.id.text_view_subject);
        viewHolder.textViewText = (TextView) convertView.findViewById(R.id.text_view_text);
        viewHolder.textViewLevel = (TextView) convertView.findViewById(R.id.text_view_level);
        viewHolder.textViewTimestamp = (TextView) convertView.findViewById(R.id.text_view_timestamp);
        viewHolder.expandIcon = (ImageView) convertView.findViewById(R.id.expand_icon);

        convertView.setTag(viewHolder);

        return convertView;
    }

    public void bindView(View view, Context context, Cursor cursor) {
        if (cursor.getCount() != 0) {
            long id = cursor.getLong(cursor.getColumnIndex(DatabaseController.COL_MSG_ID));
            String subject = cursor.getString(cursor.getColumnIndex(DatabaseController.COL_MSG_SUBJECT));
            String text = cursor.getString(cursor.getColumnIndex(DatabaseController.COL_MSG_TEXT));
            String timestamp = cursor.getString(cursor.getColumnIndex(DatabaseController.COL_MSG_TIMESTAMP));
            String level = cursor.getString(cursor.getColumnIndex(DatabaseController.COL_MSG_LEVEL));
            int status = cursor.getInt(cursor.getColumnIndex(DatabaseController.COL_MSTS_STATUS));

            if (expandedMap.get(id) == null) {
                expandedMap.put(id, false);
            }

            MessagesActivity.ViewHolder viewHolder = (MessagesActivity.ViewHolder) view.getTag();

            if (viewHolder != null) {
                viewHolder.msgId = id;
                viewHolder.msgStatus = status;
                viewHolder.textViewSubject.setText(subject);
                viewHolder.textViewText.setText(text);
                viewHolder.textViewTimestamp.setText(timestamp);
                viewHolder.textViewLevel.setText(level);

                switch (level) {
                    case "CRITICAL":
                        viewHolder.textViewLevel.setTextColor(resources.getColor(R.color.colorStateCRITICAL, context.getTheme()));
                        break;
                    case "WARNING":
                        viewHolder.textViewLevel.setTextColor(resources.getColor(R.color.colorStateWARNING, context.getTheme()));
                        break;
                    case "OK":
                        viewHolder.textViewLevel.setTextColor(resources.getColor(R.color.colorStateOK, context.getTheme()));
                        break;
                }

                // choose row background depending on messages state states
                if (status == 0) {
                    view.setBackgroundColor(resources.getColor(R.color.colorListBg, context.getTheme()));
                } else view.setBackgroundColor(Color.TRANSPARENT);

                /* choose expanded/collapsed state for textViewText.
                   We store state in sparsearray for each message id
                   so we can control state for newly binded views */
                if (!expandedMap.get(id)) {
                    viewHolder.textViewText.setMaxLines(MessagesActivity.minLines);
                    viewHolder.expandIcon.setImageResource(R.drawable.ic_expand_more_black_24dp);
                }
                else {
                    int maxLines = viewHolder.textViewText.getLineCount();
                    viewHolder.textViewText.setMaxLines(maxLines);
                    viewHolder.expandIcon.setImageResource(R.drawable.ic_expand_less_black_24dp);
                }
            }
        }
    }
}