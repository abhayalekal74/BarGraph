package com.udofy.ui.view.graph;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Created by abhayalekal on 20/03/17.
 */
public class BarGraphData implements Parcelable, Comparable {
    public static final Creator<BarGraphData> CREATOR = new Creator<BarGraphData>() {
        @Override
        public BarGraphData createFromParcel(Parcel in) {
            return new BarGraphData(in);
        }

        @Override
        public BarGraphData[] newArray(int size) {
            return new BarGraphData[size];
        }
    };
    public String date;
    public String bottomMarker;
    public String rowHeader;
    public int coins;
    public int potatoes;
    public int correct;
    public int total;
    public float[] ranges;

    protected BarGraphData(Parcel in) {
        date = in.readString();
        coins = in.readInt();
        potatoes = in.readInt();
        correct = in.readInt();
        total = in.readInt();
    }

    public BarGraphData() {

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean equals = obj instanceof BarGraphData && ((BarGraphData) obj).date.equals(date);
        return equals;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(date);
        dest.writeInt(coins);
        dest.writeInt(potatoes);
        dest.writeInt(correct);
        dest.writeInt(total);
    }

    @Override
    public int compareTo(@NonNull Object o) {
        String[] dateSplits = date.split("-");
        int year = Integer.parseInt(dateSplits[0]);
        int month = Integer.parseInt(dateSplits[1]);
        int day = Integer.parseInt(dateSplits[2]);

        String[] objDateSplits = ((BarGraphData) o).date.split("-");
        int objYear = Integer.parseInt(objDateSplits[0]);
        int objMonth = Integer.parseInt(objDateSplits[1]);
        int objDay = Integer.parseInt(objDateSplits[2]);

        if (year > objYear) {
            return 1;
        } else if (year == objYear) {
            if (month > objMonth) {
                return 1;
            } else if (month == objMonth) {
                if (day > objDay) {
                    return 1;
                } else if (day == objDay) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
