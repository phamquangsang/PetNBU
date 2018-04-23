package com.petnbu.petnbu.model;

public class Photo {
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
}
