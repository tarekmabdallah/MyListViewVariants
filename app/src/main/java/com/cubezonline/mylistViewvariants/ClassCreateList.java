package com.cubezonline.mylistViewvariants;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import lb.library.SearchablePinnedHeaderListViewAdapter;
import lb.library.StringArrayAlphabetIndexer;
import lb.listviewvariants.utils.CircularContactView;
import lb.listviewvariants.utils.ContactImageUtil;
import lb.listviewvariants.utils.ImageCache;
import lb.listviewvariants.utils.async_task_thread_pool.AsyncTaskEx;
import lb.listviewvariants.utils.async_task_thread_pool.AsyncTaskThreadPool;

public class ClassCreateList {

    private Context context;
    private ContactsAdapter mAdapter;

    public ClassCreateList(Context context, ListView mListView){
        this.context  = context;
        final ArrayList<Contact> contacts = getContacts();
        Collections.sort(contacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact lhs, Contact rhs) {
                char lhsFirstLetter = TextUtils.isEmpty(lhs.displayName) ? ' ' : lhs.displayName.charAt(0);
                char rhsFirstLetter = TextUtils.isEmpty(rhs.displayName) ? ' ' : rhs.displayName.charAt(0);
                int firstLetterComparison = Character.toUpperCase(lhsFirstLetter) - Character.toUpperCase(rhsFirstLetter);
                if (firstLetterComparison == 0)
                    return lhs.displayName.compareTo(rhs.displayName);
                return firstLetterComparison;
            }
        });


        int pinnedHeaderBackgroundColor = context.getResources().getColor(getResIdFromAttribute((Activity) context, android.R.attr.colorBackground));
        mAdapter = new ClassCreateList.ContactsAdapter(contacts);
        mAdapter.setPinnedHeaderBackgroundColor(pinnedHeaderBackgroundColor);
        mAdapter.setPinnedHeaderTextColor(context.getResources().getColor(R.color.pinned_header_text));
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);



    }

    public static int getResIdFromAttribute(final Activity activity, final int attr) {
        if (attr == 0)
            return 0;
        final TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    private ArrayList<Contact> getContacts() {
        ArrayList<Contact> result = new ArrayList<>();
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; ++i) {
            Contact contact = new Contact();
            sb.delete(0, sb.length());
            int strLength = r.nextInt(10) + 1;
            for (int j = 0; j < strLength; ++j)
                switch (r.nextInt(3)) {
                    case 0:
                        sb.append((char) ('a' + r.nextInt('z' - 'a')));
                        break;
                    case 1:
                        sb.append((char) ('A' + r.nextInt('Z' - 'A')));
                        break;
                    case 2:
                        sb.append((char) ('0' + r.nextInt('9' - '0')));
                        break;
                }

            contact.displayName = sb.toString();
            result.add(contact);
        }
        return result;
    }


    private static class Contact {
        long contactId;
        Uri contactUri;
        String displayName;
        String photoId;
    }



    public void performSearch(final String queryText) {
        mAdapter.getFilter().filter(queryText);
        mAdapter.setHeaderViewVisible(TextUtils.isEmpty(queryText));
    }



    // ////////////////////////////////////////////////////////////
    // ContactsAdapter //
    // //////////////////
    public class ContactsAdapter extends SearchablePinnedHeaderListViewAdapter<Contact> {
        private ArrayList<Contact> mContacts;
        private final int CONTACT_PHOTO_IMAGE_SIZE = 0;
        private final int[] PHOTO_TEXT_BACKGROUND_COLORS;
        final AsyncTaskThreadPool mAsyncTaskThreadPool = new AsyncTaskThreadPool(1, 2, 10);

        @Override
        public CharSequence getSectionTitle(int sectionIndex) {
            return ((StringArrayAlphabetIndexer.AlphaBetSection) getSections()[sectionIndex]).getName();
        }

        public ContactsAdapter(final ArrayList<Contact> contacts) {
            setData(contacts);
            PHOTO_TEXT_BACKGROUND_COLORS = context.getResources().getIntArray(R.array.contacts_text_background_colors);

        }

        public void setData(final ArrayList<Contact> contacts) {
            this.mContacts = contacts;
            final String[] generatedContactNames = generateContactNames(contacts);
            setSectionIndexer(new StringArrayAlphabetIndexer(generatedContactNames, true));
        }

        private String[] generateContactNames(final List<Contact> contacts) {
            final ArrayList<String> contactNames = new ArrayList<String>();
            if (contacts != null)
                for (final Contact contactEntity : contacts)
                    contactNames.add(contactEntity.displayName);
            return contactNames.toArray(new String[contactNames.size()]);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final ViewHolder holder;
            final View rootView;
            if (convertView == null) {
                holder = new ViewHolder();
                rootView = ((Activity)context).getLayoutInflater().inflate(R.layout.listview_item, parent, false);
                holder.friendProfileCircularContactView = (CircularContactView) rootView
                        .findViewById(R.id.listview_item__friendPhotoImageView);
                holder.friendProfileCircularContactView.getTextView().setTextColor(0xFFffffff);
                holder.friendName = (TextView) rootView
                        .findViewById(R.id.listview_item__friendNameTextView);
                holder.headerView = (TextView) rootView.findViewById(R.id.header_text);
                rootView.setTag(holder);
            } else {
                rootView = convertView;
                holder = (ViewHolder) rootView.getTag();
            }
            final Contact contact = getItem(position);
            final String displayName = contact.displayName;
            holder.friendName.setText(displayName);
            boolean hasPhoto = !TextUtils.isEmpty(contact.photoId);
            if (holder.updateTask != null && !holder.updateTask.isCancelled())
                holder.updateTask.cancel(true);
            final Bitmap cachedBitmap = hasPhoto ? ImageCache.INSTANCE.getBitmapFromMemCache(contact.photoId) : null;
            if (cachedBitmap != null)
                holder.friendProfileCircularContactView.setImageBitmap(cachedBitmap);
            else {
                final int backgroundColorToUse = PHOTO_TEXT_BACKGROUND_COLORS[position
                        % PHOTO_TEXT_BACKGROUND_COLORS.length];
                if (TextUtils.isEmpty(displayName))
                    holder.friendProfileCircularContactView.setImageResource(R.drawable.ic_person_white_120dp,
                            backgroundColorToUse);
                else {
                    final String characterToShow = TextUtils.isEmpty(displayName) ? "" : displayName.substring(0, 1).toUpperCase(Locale.getDefault());
                    holder.friendProfileCircularContactView.setTextAndBackgroundColor(characterToShow, backgroundColorToUse);
                }
                if (hasPhoto) {
                    holder.updateTask = new AsyncTaskEx<Void, Void, Bitmap>() {

                        @Override
                        public Bitmap doInBackground(final Void... params) {
                            if (isCancelled())
                                return null;
                            final Bitmap b = ContactImageUtil.loadContactPhotoThumbnail(context, contact.photoId, CONTACT_PHOTO_IMAGE_SIZE);
                            if (b != null)
                                return ThumbnailUtils.extractThumbnail(b, CONTACT_PHOTO_IMAGE_SIZE,
                                        CONTACT_PHOTO_IMAGE_SIZE);
                            return null;
                        }

                        @Override
                        public void onPostExecute(final Bitmap result) {
                            super.onPostExecute(result);
                            if (result == null)
                                return;
                            ImageCache.INSTANCE.addBitmapToCache(contact.photoId, result);
                            holder.friendProfileCircularContactView.setImageBitmap(result);
                        }
                    };
                    mAsyncTaskThreadPool.executeAsyncTask(holder.updateTask);
                }
            }
            bindSectionHeader(holder.headerView, null, position);
            return rootView;
        }

        @Override
        public boolean doFilter(final Contact item, final CharSequence constraint) {
            if (TextUtils.isEmpty(constraint))
                return true;
            final String displayName = item.displayName;
            return !TextUtils.isEmpty(displayName) && displayName.toLowerCase(Locale.getDefault())
                    .contains(constraint.toString().toLowerCase(Locale.getDefault()));
        }

        @Override
        public ArrayList<Contact> getOriginalList() {
            return mContacts;
        }


    }

    // /////////////////////////////////////////////////////////////////////////////////////
    // ViewHolder //
    // /////////////
    private static class ViewHolder {
        public CircularContactView friendProfileCircularContactView;
        TextView friendName, headerView;
        public AsyncTaskEx<Void, Void, Bitmap> updateTask;
    }
}
