package com.berkesoft.javamaps.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

//ROOMlar sayesinde daha kolay ve kullanışlı database yapabiliriz.
//6) İlk önce bir sınıf oluşturuyoruz ve Sınıfın üstüne @Entity yazıyoruz ki room olduğu anlaşılsın.
@Entity
public class Place implements Serializable {

    //6.1) id nin başına @Primary key yazıyoruz ve (autoGenerate = true) diyerek otomatik id atmasını sağlıyoruz.
    @PrimaryKey (autoGenerate = true)
    public int id;

    //6.2) (name = "name") => bu SQL deki column isimleri.
    @ColumnInfo (name = "name")
    public String name;

    @ColumnInfo (name = "latitude")
    public Double latitude;

    @ColumnInfo (name = "longitude")
    public Double longitude;

    //6.3) En son constructor yapıp olayı bitiriyorum.
    public Place(String name, Double latitude, Double longitude){
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
