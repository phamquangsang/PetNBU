package com.petnbu.petnbu.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Photo implements Parcelable {

    private String originUrl;   //for larger than FHD
    private String largeUrl;    //for FHD
    private String mediumUrl;   //for HD
    private String smallUrl;    //for qHD
    private String thumbnailUrl;//for thumbnail
    private int width;
    private int height;

    public Photo() {
    }

    public Photo(String originUrl, String largeUrl, String smallUrl, String thumbnailUrl, int width, int height) {
        this.originUrl = originUrl;
        this.largeUrl = largeUrl;
        this.smallUrl = smallUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.width = width;
        this.height = height;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getLargeUrl() {
        return largeUrl;
    }

    public void setLargeUrl(String largeUrl) {
        this.largeUrl = largeUrl;
    }

    public String getSmallUrl() {
        return smallUrl;
    }

    public void setSmallUrl(String smallUrl) {
        this.smallUrl = smallUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Photo photo = (Photo) o;

        if (width != photo.width) return false;
        if (height != photo.height) return false;
        if (originUrl != null ? !originUrl.equals(photo.originUrl) : photo.originUrl != null)
            return false;
        if (largeUrl != null ? !largeUrl.equals(photo.largeUrl) : photo.largeUrl != null)
            return false;
        if (mediumUrl != null ? !mediumUrl.equals(photo.mediumUrl) : photo.mediumUrl != null)
            return false;
        if (smallUrl != null ? !smallUrl.equals(photo.smallUrl) : photo.smallUrl != null)
            return false;
        return thumbnailUrl != null ? thumbnailUrl.equals(photo.thumbnailUrl) : photo.thumbnailUrl == null;
    }

    @Override
    public int hashCode() {
        int result = originUrl != null ? originUrl.hashCode() : 0;
        result = 31 * result + (largeUrl != null ? largeUrl.hashCode() : 0);
        result = 31 * result + (mediumUrl != null ? mediumUrl.hashCode() : 0);
        result = 31 * result + (smallUrl != null ? smallUrl.hashCode() : 0);
        result = 31 * result + (thumbnailUrl != null ? thumbnailUrl.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "Photo{" +
                "originUrl='" + originUrl + '\'' +
                ", largeUrl='" + largeUrl + '\'' +
                ", mediumUrl='" + mediumUrl + '\'' +
                ", smallUrl='" + smallUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.originUrl);
        dest.writeString(this.largeUrl);
        dest.writeString(this.mediumUrl);
        dest.writeString(this.smallUrl);
        dest.writeString(this.thumbnailUrl);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    protected Photo(Parcel in) {
        this.originUrl = in.readString();
        this.largeUrl = in.readString();
        this.mediumUrl = in.readString();
        this.smallUrl = in.readString();
        this.thumbnailUrl = in.readString();
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public static final Creator<Photo> CREATOR = new Creator<Photo>() {
        @Override
        public Photo createFromParcel(Parcel source) {
            return new Photo(source);
        }

        @Override
        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };
}
