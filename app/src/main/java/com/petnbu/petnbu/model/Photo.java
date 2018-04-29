package com.petnbu.petnbu.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Photo implements Parcelable {

    private String originUrl;
    private String largeUrl;
    private String smallUrl;
    private String thumbnailUrl;
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

        if (getWidth() != photo.getWidth()) return false;
        if (getHeight() != photo.getHeight()) return false;
        if (getOriginUrl() != null ? !getOriginUrl().equals(photo.getOriginUrl()) : photo.getOriginUrl() != null)
            return false;
        if (getLargeUrl() != null ? !getLargeUrl().equals(photo.getLargeUrl()) : photo.getLargeUrl() != null)
            return false;
        if (getSmallUrl() != null ? !getSmallUrl().equals(photo.getSmallUrl()) : photo.getSmallUrl() != null)
            return false;
        return getThumbnailUrl() != null ? getThumbnailUrl().equals(photo.getThumbnailUrl()) : photo.getThumbnailUrl() == null;
    }

    @Override
    public int hashCode() {
        int result = getOriginUrl() != null ? getOriginUrl().hashCode() : 0;
        result = 31 * result + (getLargeUrl() != null ? getLargeUrl().hashCode() : 0);
        result = 31 * result + (getSmallUrl() != null ? getSmallUrl().hashCode() : 0);
        result = 31 * result + (getThumbnailUrl() != null ? getThumbnailUrl().hashCode() : 0);
        result = 31 * result + getWidth();
        result = 31 * result + getHeight();
        return result;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.originUrl);
        dest.writeString(this.largeUrl);
        dest.writeString(this.smallUrl);
        dest.writeString(this.thumbnailUrl);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    protected Photo(Parcel in) {
        this.originUrl = in.readString();
        this.largeUrl = in.readString();
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

    @Override
    public String toString() {
        return "Photo{" +
                "originUrl='" + originUrl + '\'' +
                ", largeUrl='" + largeUrl + '\'' +
                ", smallUrl='" + smallUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
