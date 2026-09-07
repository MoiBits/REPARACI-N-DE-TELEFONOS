package com.example;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PhoneSaleDao {
    @Insert
    long insert(PhoneSale sale);

    @Update
    void update(PhoneSale sale);

    @Delete
    void delete(PhoneSale sale);

    @Query("SELECT * FROM phone_sales ORDER BY saleDate DESC")
    List<PhoneSale> getAllSales();

    @Query("SELECT * FROM phone_sales WHERE id = :id")
    PhoneSale getSaleById(int id);

    @Query("SELECT SUM(price) FROM phone_sales")
    double getTotalSalesAmount();
}
