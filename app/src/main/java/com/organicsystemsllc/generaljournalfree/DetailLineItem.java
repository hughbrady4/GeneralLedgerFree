package com.organicsystemsllc.generaljournalfree;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.NumberFormat;

class DetailLineItem implements Parcelable {

    public static final Parcelable.Creator<DetailLineItem> CREATOR =
            new Creator<DetailLineItem>() {

                @Override
                public DetailLineItem createFromParcel(Parcel source) {
                    return new DetailLineItem(source);
                }

                @Override
                public DetailLineItem[] newArray(int size) {
                    return new DetailLineItem[size];
                }
            };
    private String mType;
    private String mAccount;
    private double mAmount;

    DetailLineItem(String type, double amount, String account) {
        this.mType = type;
        this.mAccount = account;
        this.mAmount = amount;
    }

    private DetailLineItem(Parcel source) {
        String[] detailLineItem = new String[3];
        source.readStringArray(detailLineItem);
        this.mType = detailLineItem[0];
        this.mAccount = detailLineItem[1];
        this.mAmount = source.readDouble();

    }

    @Override
    public String toString() {
        return mType + " " + mAccount + " " + NumberFormat.getCurrencyInstance().format(mAmount);
    }

    double getAmount() {
        return mAmount;
    }

    public void setAmount(double mAmount) {
        this.mAmount = mAmount;
    }

    String getAccount() {
        return mAccount;
    }

    public void setAccount(String mAccount) {
        this.mAccount = mAccount;
    }

    public String getType() {
        return mType;
    }

    public void setType(String mType) {
        this.mType = mType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] contents = new String[]{this.mType, this.mAccount};
        dest.writeStringArray(contents);
        dest.writeDouble(this.mAmount);
    }

}
