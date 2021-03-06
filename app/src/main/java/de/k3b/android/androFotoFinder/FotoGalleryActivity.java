/*
 * Copyright (c) 2015-2018 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
 
package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.directory.DirectoryGui;
import de.k3b.android.androFotoFinder.gallery.cursor.GalleryCursorFragment;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.locationmap.GeoEditActivity;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.queries.Queryable;
import de.k3b.android.util.GarbageCollector;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.BaseQueryActivity;
import de.k3b.database.QueryParameter;
import de.k3b.io.collections.SelectedItems;
import de.k3b.io.IDirectory;

/**
 * Gallery: Show zeoro or more images in a grid optionally filtered by a
 * * base query plus
 * * sub query (either path or date or geo or tag or in-any)
 *
 * Use cases
 * * stand alone gallery (i.e. from file manager with intent-uri of image.jpg/virtual-album/directory)
 * * sub gallery drill down from other activity with intent containing file-uri/query-extra and/or filter-extra
 * * as picker for
 * * * ACTION_PICK uri=geo:... pick geo via image
 * * * ACTION_PICK pick image old android-2.1 api
 * * * ACTION_GET_CONTENT image new android-4.4 api
 */
public class FotoGalleryActivity extends BaseQueryActivity implements
        OnGalleryInteractionListener {
    /**
     * intent parameters supported by FotoGalleryActivity: EXTRA_...
     */

    // multi selection support
    private SelectedItems mSelectedItems = null;

    private Queryable mGalleryGui;

    private DirectoryGui mDirGui;

    /**
     * shows a new instance of FotoGalleryActivity.
     * @param debugContext
     * @param context     calling activity
     * @param query       if != null set initial filter to new FotoGalleryActivity
     * @param requestCode if != 0 start for result. else start without result
     */
    public static void showActivity(String debugContext, Activity context, QueryParameter query, int requestCode) {
        Intent intent = new Intent(context, FotoGalleryActivity.class);

        AndroidAlbumUtils.saveFilterAndQuery(context, null, intent, null, null, query);

        IntentUtil.startActivity(debugContext, context, requestCode, intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        if (BuildConfig.DEBUG) {
            // not implemented yet
            FotoLibGlobal.itpcWriteSupport = false;
        }
        if (Global.debugEnabled && (intent != null)) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        setContentView(R.layout.activity_gallery); // .gallery_activity);

        onCreateData(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        mGalleryGui = (Queryable) fragmentManager.findFragmentById(R.id.galleryCursor);

        if (mGalleryGui instanceof GalleryCursorFragment) {
            this.mSelectedItems = ((GalleryCursorFragment) mGalleryGui).getSelectedItems();
        }

        // on tablet seperate dir navigator fragment
        mDirGui = (DirectoryGui) fragmentManager.findFragmentById(R.id.directoryFragment);

        if (FotoViewerParameter.galleryHasEmbeddedDirPicker) {
            if (mDirGui == null) {
                // on small screen/cellphone DirectoryGui is part of gallery
                mDirGui = (DirectoryGui) fragmentManager.findFragmentById(R.id.galleryCursor);
            } else {
                mHasEmbeddedDirPicker = true;
            }
        } else {
            if (mDirGui != null) {
                fragmentManager.beginTransaction().remove((Fragment) mDirGui).commit();
                mDirGui = null;
            }
        }

        setTitle();
        reloadGui("onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGalleryGui instanceof GalleryCursorFragment) {
            this.mSelectedItems = ((GalleryCursorFragment) mGalleryGui).getSelectedItems();
        }
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(mDebugPrefix, "onDestroy start");
        super.onDestroy();

        // to avoid memory leaks
        GarbageCollector.freeMemory(findViewById(R.id.root_view));
        mGalleryGui = null;
        mDirGui = null;

        System.gc();
        Global.debugMemory(mDebugPrefix, "onDestroy end");
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent intent = this.getIntent();
        String action = (intent != null) ? intent.getAction() : null;
        if ((action == null) || (((Intent.ACTION_PICK.compareTo(action) != 0) && (Intent.ACTION_GET_CONTENT.compareTo(action) != 0)))) {
            MenuInflater inflater = getMenuInflater();

            inflater.inflate(R.menu.menu_gallery_non_selected_only, menu);
            inflater.inflate(R.menu.menu_gallery_non_multiselect, menu);
            /*
            getActionBar().setListNavigationCallbacks();
            MenuItem sorter = menu.getItem(R.id.cmd_sort);
            sorter.getSubMenu().
            */
            Global.fixMenu(this, menu);
        }

        final boolean result = super.onCreateOptionsMenu(menu);
        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sorter = menu.findItem(R.id.cmd_sort);

        if (sorter != null) {
            String sortTitle = getString(R.string.sort_menu_title) +
                    ": " +
                    mGalleryQueryParameter.getSortDisplayName(this);
            sorter.setTitle(sortTitle);
        }
        AboutDialogPreference.onPrepareOptionsMenu(this, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cmd_settings:
                SettingsActivity.showActivity(this);
                return true;
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_more:
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        // reopen after some delay
                        openOptionsMenu();
                    }
                }, 200);
                return true;
            default:
                return onOptionsItemSelected(item, this.mSelectedItems);
        }

    }

    /**
     * Call back from sub-activities.<br/>
     * Process Change StartTime (longpress start), Select StopTime before stop
     * (longpress stop) or filter change for detailReport
     */
    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (mGalleryGui instanceof Fragment) {
            ((Fragment) mGalleryGui).onActivityResult(requestCode, resultCode, intent);
        }

        switch (requestCode) {
            case ImageDetailActivityViewPager.ACTIVITY_ID:
                if (resultCode == ImageDetailActivityViewPager.RESULT_CHANGE) {
                    invalidateDirectories(mDebugPrefix + "#onActivityResult from ImageDetailActivityViewPager");
                }
                break;
            case GeoEditActivity.RESULT_ID:
                if (resultCode == ImageDetailActivityViewPager.RESULT_CHANGE) {
                    invalidateDirectories(mDebugPrefix + "#onActivityResult from GeoEditActivity");
                }
                break;
            default:
                break;
        }
    }

    /** called by Fragment: a fragment Item was clicked */
    @Override
    public void onGalleryImageClick(long imageId, Uri imageUri, int position) {
        Global.debugMemory(mDebugPrefix, "onGalleryImageClick");
        QueryParameter imageDetailQuery = this.mGalleryQueryParameter.calculateEffectiveGalleryContentQuery();
        ImageDetailActivityViewPager.showActivity("[1]#" + imageId, this, imageUri, position, imageDetailQuery, ImageDetailActivityViewPager.ACTIVITY_ID);
    }

    @Override
    protected void defineDirectoryNavigation(IDirectory directoryRoot) {
        if (mDirGui != null) {
            mDirGui.defineDirectoryNavigation(directoryRoot, mGalleryQueryParameter.getDirQueryID(),
                    mGalleryQueryParameter.getCurrentSubFilterSettings().getPath());
        }
    }

    @Override
    protected void reloadGui(String why) {
        if (mGalleryGui != null) {
            QueryParameter query = this.mGalleryQueryParameter.calculateEffectiveGalleryContentQuery();
            if (query != null) {
                this.mGalleryGui.requery(this, query, mDebugPrefix + why);
            }
        }

        if (mDirGui != null) {
            String currentPath = this.mGalleryQueryParameter.getCurrentSubFilterSettings().getPath();
            if (currentPath != null) {
                mDirGui.navigateTo(currentPath);
            }
        }
    }

    @Override
    public String toString() {
        return mDebugPrefix + "->" + this.mGalleryGui;
    }


}
