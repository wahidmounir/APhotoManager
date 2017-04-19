/*
 * Copyright (c) 2017 by k3b.
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
package de.k3b.media;

import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegCommentDirectory;
import com.drew.metadata.xmp.XmpDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import de.k3b.FotoLibGlobal;
import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;

/**
 * com.drewnoakes:metadata-extractor based reader for image meta data files
 * Created by k3b on 27.03.2017.
 */

public class ImageMetaReader implements IMetaApi, Closeable {
    // public: used as log filter for crash report
    public  static final String LOG_TAG = "ImageMetaReader";

    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);

    // public: can be changed in settings dialog
    public  static boolean DEBUG = false;
    private static final boolean DEBUG_ALWAYS_NULL = false; // debug-time to enfaorce all meta readings

    private String mFilename = null;
    private IMetaApi mExternalXmpDir;
    private MediaXmpSegment mInternalXmpDir;
    private Metadata mMetadata = null;
    private Directory mExifDir;
    private GeoLocation mExifGpsDir;
    private Directory mIptcDir;
    // private Directory fileDir;
    private Directory mCommentDir;
    private String dbg_context = "";

    /**
     * Reads Meta data from the specified inputStream (if not null) or File(filename).
     */
    public ImageMetaReader load(String filename, InputStream inputStream, IMetaApi externalXmpContent, String _dbg_context) throws IOException {
        mFilename = filename;
        mExternalXmpDir = externalXmpContent;
        this.dbg_context = _dbg_context + "->ImageMetaReader(" + mFilename+ ") ";

        Metadata metadata = null;
        File jpegFile = (inputStream == null) ? new File(filename) : null;
        try {
            if (inputStream != null) {
                // so proguard can eleminate support for gif, png and other image formats
                metadata = JpegMetadataReader.readMetadata(inputStream);
                // metadata = ImageMetadataReader.readMetadata(inputStream);
            } else {
                metadata = JpegMetadataReader.readMetadata(jpegFile);
                // metadata = ImageMetadataReader.readMetadata(jpegFile);
            }
            // IptcDirectory.TAG_ARM_VERSION
        } catch (ImageProcessingException e) {
            logger.error(dbg_context +" Error open file " + e.getMessage(), e);

            metadata = null;
        }
        mMetadata = metadata;

        if (metadata == null) {
            if (FotoLibGlobal.debugEnabledJpgMetaIo) {
                logger.debug(dbg_context +
                        "load: file not found ");
            }
            return null;
        }

        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(dbg_context +
                    "loaded: " + MediaUtil.toString(this));
        }

        return this;
    }

    private static final String NL = "\n";

    @Override
    public String getPath() {
        return mFilename;
    }

    @Override
    public IMetaApi setPath(String filePath) {
        throw new UnsupportedOperationException ();
    }

    /**
     * When the photo was taken (not file create/modify date) in local time or utc
     */
    @Override
    public Date getDateTimeTaken() {
        String debugContext = "getDateTimeTaken";
        int i=0;

        init();

        Date result = null;
        if (isEmpty(result, debugContext, ++i) && (mExifDir != null)) result = mExifDir.getDate(ExifIFD0Directory.TAG_DATETIME_ORIGINAL, DateUtil.UTC);

        if (isEmpty(result, debugContext, ++i) && (mExternalXmpDir != null)) result = mExternalXmpDir.getDateTimeTaken();
        if (isEmpty(result, debugContext, ++i) && (mInternalXmpDir != null)) result = mInternalXmpDir.getDateTimeTaken();
        return result;
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Latitude, in degrees north.
     *
     * @param latitude
     */
    @Override
    public IMetaApi setLatitude(Double latitude) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Longitude, in degrees east.
     *
     * @param longitude
     */
    @Override
    public IMetaApi setLongitude(Double longitude) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Double getLatitude() {
        String debugContext = "getLatitude";
        int i=0;
        init();

        Double result = null;
        if (isEmpty(result, debugContext, ++i) && (mExifGpsDir != null)) result = mExifGpsDir.getLatitude();

        if (isEmpty(result, debugContext, ++i) && (mExternalXmpDir != null)) result = mExternalXmpDir.getLatitude();
        if (isEmpty(result, debugContext, ++i) && (mInternalXmpDir != null)) result = mInternalXmpDir.getLatitude();
        return result;
    }

    @Override
    public Double getLongitude() {
        String debugContext = "getLongitude";
        int i=0;

        init();

        Double result = null;
        if (isEmpty(result, debugContext, ++i) && (mExifGpsDir != null)) result = mExifGpsDir.getLongitude();

        if (isEmpty(result, debugContext, ++i) && (mExternalXmpDir != null)) result = mExternalXmpDir.getLongitude();
        if (isEmpty(result, debugContext, ++i) && (mInternalXmpDir != null)) result = mInternalXmpDir.getLongitude();
        return result;
    }

    /**
     * Title = Short Descrioption used as caption
     */
    @Override
    public String getTitle() {
        String debugContext = "getTitle";
        int i=0;

        init();

        String result = null;

        if (isEmpty(result, debugContext, ++i) && (mExternalXmpDir != null)) result = mExternalXmpDir.getTitle();

        if (isEmpty(result, debugContext, ++i)) result = getString(debugContext, mExifDir, ExifDirectoryBase.TAG_WIN_TITLE);
        //=> XPTitle

        if ((isEmpty(result, debugContext, ++i)) && (mInternalXmpDir != null)) result = mInternalXmpDir.getTitle();

        if (isEmpty(result, debugContext, ++i)) result = getString(debugContext, mIptcDir, IptcDirectory.TAG_HEADLINE);
        // => Headline

        return result;
    }

    private static boolean isEmpty(Object result, String debugContext, int tryNumber) {
        if (DEBUG_ALWAYS_NULL) {
            logger.info(debugContext + "#" + tryNumber + ":" +result);
            return true;
        }
        return result == null;
    }

    @Override
    public IMetaApi setTitle(String title) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Longer description = comment. may have more than one line
     */
    @Override
    public String getDescription() {
        String debugContext = "getDescription";
        int i=0;

        init();
        String result = null;

        if (isEmpty(result, debugContext, ++i)) result = getString(debugContext, mExifDir, ExifDirectoryBase.TAG_IMAGE_DESCRIPTION);
        // => ImageDescription

        if ((isEmpty(result, debugContext, ++i)) && (mExternalXmpDir != null)) result = mExternalXmpDir.getDescription();

        if (isEmpty(result, debugContext, ++i)) result = getString(debugContext, mExifDir, ExifDirectoryBase.TAG_WIN_COMMENT);
        // => Comment

        if ((isEmpty(result, debugContext, ++i)) && (mInternalXmpDir != null)) result = mInternalXmpDir.getDescription();

        //!!! not implemented in  com.drewnoakes:metadata-extractor:2.8.1
        //!!! if isEmpty(result) result = getString(debugContext, mInternalXmpDir, XmpDirectory.TAG_DESCRIPTION);

        if (isEmpty(result, debugContext, ++i)) result = getString(debugContext, mIptcDir, IptcDirectory.TAG_CAPTION);
        // => Caption-Abstract

        if (isEmpty(result, debugContext, ++i)) result = getString(debugContext, mCommentDir, JpegCommentDirectory.TAG_COMMENT);
        // => Comment

        //!!! not implemented in  com.drewnoakes:metadata-extractor:2.8.1
        // not supported -XMP-dc:Description < File:Comment
        return result;
    }

    @Override
    public IMetaApi setDescription(String description) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Tags/Keywords/Categories/VirtualAlbum used to find images
     */
    @Override
    public List<String> getTags() {
        String debugContext = "getTags";
        int i=0;

        init();
        List<String> result = null;

        if ((isEmpty(result, debugContext, ++i)) && (mExternalXmpDir != null)) result = mExternalXmpDir.getTags();

        mExifDir.getDescription(ExifDirectoryBase.TAG_WIN_KEYWORDS);
        if (isEmpty(result, debugContext, ++i) && (mExifDir != null)) {
            // result = ListUtils.toStringList(mExifDir.getStringValueArray(ExifDirectoryBase.TAG_WIN_KEYWORDS));
            String value = mExifDir.getDescription(ExifDirectoryBase.TAG_WIN_KEYWORDS);
            if (value != null) {
                result = ListUtils.toStringList((String[]) value.split(";"));
            }
        }

        if ((isEmpty(result, debugContext, ++i)) && (mInternalXmpDir != null)) result = mInternalXmpDir.getTags();

        return result;
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        throw new UnsupportedOperationException ();
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        String debugContext = "getRating";
        int i=0;
        init();

        Integer result = null;
        if (isEmpty(result, debugContext, ++i) && (mExternalXmpDir != null)) result = mExternalXmpDir.getRating();
        if (isEmpty(result, debugContext, ++i) && (mInternalXmpDir != null)) result = mInternalXmpDir.getRating();
        if (isEmpty(result, debugContext, ++i) && (mExifGpsDir != null)) result = mExifDir.getInteger(ExifDirectoryBase.TAG_RATING);
        return result;
    }

    @Override
    public IMetaApi setRating(Integer value) {
        throw new UnsupportedOperationException ();
    }

    /** return the image orinentation as id (one of the ORIENTATION_ROTATE_XXX constants) */
    private Integer getOrientationId() {
        return mExifDir.getInteger(ExifDirectoryBase.TAG_ORIENTATION);
    }

    private static final int ORIENTATION_ROTATE_180 = 3;
    private static final int ORIENTATION_ROTATE_90 = 6;  // rotate 90 cw to right it
    private static final int ORIENTATION_ROTATE_270 = 8;  // rotate 270 to right it

    /** return image orinentation in degrees (0, 90,180,270) or 0 if inknown */
    public int getOrientationInDegrees() {
        Integer orientation = getOrientationId();
        if (orientation != null) {
            // We only recognize a subset of orientation tag values.
            int degree;
            switch (orientation.intValue()) {
                case ORIENTATION_ROTATE_90:
                    return 90;
                case ORIENTATION_ROTATE_180:
                    return 180;
                case ORIENTATION_ROTATE_270:
                    return 270;
                default:
            }
        }
        return 0;
    }



    @Override
    public void close() throws IOException {

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Directory directory : mMetadata.getDirectories()) {
            String dirName = "";
            Directory parent = directory;
            while (parent != null) {
                dirName = parent.getName() + "." + dirName;
                parent = null; // directory.getParent(); requires newer version
            }

            /*
            builder.append(NL).append(dirName).append(":")
                    .append(directory.getClass().getSimpleName()).append(NL);
            */

            for (Tag tag : directory.getTags()) {
                int tagType = tag.getTagType();
                appendValue(builder, directory, dirName, tagType);
                builder.append(NL);
            }
        }

        if (mInternalXmpDir == null) {
            XmpDirectory xmp = this.mMetadata.getFirstDirectoryOfType(XmpDirectory.class);
            if (xmp != null) {
                mInternalXmpDir = new MediaXmpSegment();
                mInternalXmpDir.setXmpMeta(xmp.getXMPMeta(), dbg_context + " embedded xml ");
                mInternalXmpDir.appendXmp("xmp.", builder);
            }
        }

        return builder.toString();
    }

    //--------------- local helpers

    private StringBuilder appendValue(StringBuilder result, Directory directory, String dirName, int tagType) {
        String tagValue = (directory == null) ? null  : directory.getDescription(tagType);
        String tagName  = (directory == null) ? "???" : directory.getTagName(tagType);
        if (tagValue == null)
            tagValue = "";

        result.append(dirName).append(tagName);
        if (DEBUG) {
            result.append("(0x").append( Integer.toHexString(tagType)).append(")");
        }
        result.append("=").append(tagValue);
        return result;
    }

    private void init() {
        mExifDir = this.mMetadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        mIptcDir = this.mMetadata.getFirstDirectoryOfType(IptcDirectory.class);
        // fileDir = this.mMetadata.getFirstDirectoryOfType(FileDirec .class); // not implemented
        mCommentDir = this.mMetadata.getFirstDirectoryOfType(JpegCommentDirectory.class);

        GpsDirectory gps = this.mMetadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gps != null) {
            mExifGpsDir = gps.getGeoLocation();
            if (mExifGpsDir.isZero()) mExifGpsDir = null;
        }

        XmpDirectory xmp = this.mMetadata.getFirstDirectoryOfType(XmpDirectory.class);
        if (xmp != null) {
            mInternalXmpDir = new MediaXmpSegment();
            mInternalXmpDir.setXmpMeta(xmp.getXMPMeta(), dbg_context + " embedded xml ");
        }
    }

    private String getString(String debugContext, Directory directory, int tagType) {
        String result = null;
        if (directory != null) {
            result = directory.getDescription(tagType);
            if (DEBUG) {
                StringBuilder dbg = new StringBuilder();
                dbg.append(debugContext).append(":");
                appendValue(dbg, directory, directory.getName() + ".", tagType);
                logger.info(dbg.toString());
            }
        }
        if ((result != null) && (result.length() == 0)) return null;
        return result;
    }
}