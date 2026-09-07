package com.example;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface RepairOrderDao {
    @Insert
    long insert(RepairOrder order);

    @Update
    void update(RepairOrder order);

    @Delete
    void delete(RepairOrder order);

    @Query("SELECT * FROM repair_orders WHERE id = :id")
    RepairOrder getOrderById(int id);

    @Query("SELECT * FROM repair_orders ORDER BY entryDate DESC")
    List<RepairOrder> getAllOrders();

    @Query("SELECT * FROM repair_orders WHERE status = :status")
    List<RepairOrder> getOrdersByStatus(String status);

    @Query("SELECT COUNT(*) FROM repair_orders WHERE status = :status")
    int getCountByStatus(String status);

    @Query("SELECT SUM(estimatedPrice) FROM repair_orders WHERE paymentStatus = 'Cancelado'")
    double getTotalEarned();

    @Query("SELECT SUM(estimatedPrice) FROM repair_orders WHERE paymentStatus = 'Por Pagar'")
    double getTotalPending();

    @Query("SELECT * FROM repair_orders ORDER BY entryDate DESC LIMIT 5")
    List<RepairOrder> getLatestOrders();

    @Query("SELECT * FROM repair_orders WHERE customerName LIKE '%' || :query || '%' OR deviceBrand LIKE '%' || :query || '%' OR deviceModel LIKE '%' || :query || '%' OR imei LIKE '%' || :query || '%'")
    List<RepairOrder> searchOrders(String query);
}
