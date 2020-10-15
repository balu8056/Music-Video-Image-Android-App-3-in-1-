package com.example.musicv2.data

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class ImageByFolder(val imageFolder: String?, val imageList: ArrayList<ImageData>?)
data class ImageData(val imageId: Long, val name: String?, val data: String?, val folderName: String?, val date: Long): Parcelable{
    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readLong())
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(imageId)
        parcel.writeString(name)
        parcel.writeString(data)
        parcel.writeString(folderName)
        parcel.writeLong(date)
    }
    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<ImageData> {
        override fun createFromParcel(parcel: Parcel): ImageData = ImageData(parcel)
        override fun newArray(size: Int): Array<ImageData?> = arrayOfNulls(size)
    }
}

data class VideoByFolder(val videoFolder: String?, val videoList: ArrayList<VideoData>?)
data class VideoData(val videoId: Long, val name: String?, val duration: Long, val date: Long, val folderName: String?): Parcelable{
    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readString(), parcel.readLong(), parcel.readLong(), parcel.readString())
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(videoId)
        parcel.writeString(name)
        parcel.writeLong(duration)
        parcel.writeLong(date)
        parcel.writeString(folderName)
    }
    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<VideoData> {
        override fun createFromParcel(parcel: Parcel): VideoData = VideoData(parcel)
        override fun newArray(size: Int): Array<VideoData?> = arrayOfNulls(size)
    }
}

data class MusicData(val musicId: Long, val musicArtUri: Uri?, val musicName: String?, val musicAlbum: String?, var duration:Long): Parcelable {
    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readParcelable(Uri::class.java.classLoader), parcel.readString(), parcel.readString(), parcel.readLong())
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(musicId)
        parcel.writeParcelable(musicArtUri, flags)
        parcel.writeString(musicName)
        parcel.writeString(musicAlbum)
        parcel.writeLong(duration)
    }
    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<MusicData> {
        override fun createFromParcel(parcel: Parcel): MusicData = MusicData(parcel)
        override fun newArray(size: Int): Array<MusicData?> = arrayOfNulls(size)
    }
}