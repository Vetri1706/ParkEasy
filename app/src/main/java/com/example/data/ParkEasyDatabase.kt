package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val type: String, // Bike, Car, SUV, EV
    val brand: String,
    val model: String,
    val color: String
)

@Entity(tableName = "parking_lots")
data class ParkingLot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,
    val totalSlots: Int,
    val availableSlots: Int,
    val pricePerHour: Double,
    val distanceMeters: Int,
    val hasCCTV: Boolean,
    val isCovered: Boolean,
    val hasEVCharging: Boolean,
    val hasSecurity: Boolean,
    val hasWashroom: Boolean,
    val openingHours: String,
    val rating: Double,
    val weekdayPrice: Double,
    val weekendPrice: Double,
    val festivalPrice: Double,
    val peakHoursPrice: Double,
    val isApproved: Boolean = true,
    val isBlocked: Boolean = false,
    // Slot breakdowns
    val bikeSlotsTotal: Int = 10,
    val bikeSlotsAvailable: Int = 10,
    val carSlotsTotal: Int = 6,
    val carSlotsAvailable: Int = 6,
    val suvSlotsTotal: Int = 2,
    val suvSlotsAvailable: Int = 2,
    val evSlotsTotal: Int = 2,
    val evSlotsAvailable: Int = 2,
    val isSavedAsFavorite: Boolean = false
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: String, // PK-XXXXXX
    val parkingLotId: Long,
    val parkingLotName: String,
    val vehicleId: Long,
    val vehicleNumber: String,
    val vehicleType: String,
    val date: String,
    val time: String,
    val durationHours: Int,
    val totalPrice: Double,
    val qrCodeString: String,
    val status: String, // Upcoming, CheckedIn, Completed, Cancelled
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val finalBill: Double = 0.0,
    val cleanlinessRating: Int = 0,
    val securityRating: Int = 0,
    val easeOfEntryRating: Int = 0,
    val comments: String = "",
    val isEvChargingReserved: Boolean = false
)

@Entity(tableName = "partners")
data class Partner(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val mobileNumber: String,
    val aadhaar: String,
    val pan: String,
    val shopLicense: String,
    val bankAccount: String,
    val status: String, // Pending, Approved, Rejected
    val shopPhotoUrl: String = "",
    val parkingPhotoUrl: String = ""
)

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey val id: Int = 1,
    val balance: Double = 520.0,
    val loyaltyPoints: Int = 100
)

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // Add, Refund, Spend, Cashback, Referral
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_config")
data class SystemConfig(
    @PrimaryKey val id: Int = 1,
    val commissionPercentage: Double = 30.0 // Park Easy takes 30% by default
)

@Dao
interface ParkEasyDao {
    // Vehicles
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    // Parking Lots
    @Query("SELECT * FROM parking_lots")
    fun getAllParkingLots(): Flow<List<ParkingLot>>

    @Query("SELECT * FROM parking_lots WHERE id = :id")
    suspend fun getParkingLotById(id: Long): ParkingLot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParkingLot(parkingLot: ParkingLot)

    @Update
    suspend fun updateParkingLot(parkingLot: ParkingLot)

    @Delete
    suspend fun deleteParkingLot(parkingLot: ParkingLot)

    // Bookings
    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: Long): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)

    // Partners
    @Query("SELECT * FROM partners ORDER BY id DESC")
    fun getAllPartners(): Flow<List<Partner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: Partner)

    @Update
    suspend fun updatePartner(partner: Partner)

    // Wallet
    @Query("SELECT * FROM wallet WHERE id = 1")
    fun getWalletFlow(): Flow<Wallet?>

    @Query("SELECT * FROM wallet WHERE id = 1")
    suspend fun getWallet(): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet)

    // Wallet Transactions
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getWalletTransactions(): Flow<List<WalletTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransaction(transaction: WalletTransaction)

    // System Config
    @Query("SELECT * FROM system_config WHERE id = 1")
    fun getSystemConfigFlow(): Flow<SystemConfig?>

    @Query("SELECT * FROM system_config WHERE id = 1")
    suspend fun getSystemConfig(): SystemConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemConfig(config: SystemConfig)
}

@Database(
    entities = [
        Vehicle::class,
        ParkingLot::class,
        Booking::class,
        Partner::class,
        Wallet::class,
        WalletTransaction::class,
        SystemConfig::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ParkEasyDatabase : RoomDatabase() {
    abstract fun dao(): ParkEasyDao

    companion object {
        @Volatile
        private var INSTANCE: ParkEasyDatabase? = null

        fun getDatabase(context: Context): ParkEasyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ParkEasyDatabase::class.java,
                    "park_easy_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                
                // Seed database asynchronously or in thread if empty
                Thread {
                    seedDatabaseIfEmpty(instance)
                }.start()

                instance
            }
        }

        private fun seedDatabaseIfEmpty(db: ParkEasyDatabase) {
            val dao = db.dao()
            runBlocking {
                val existingLots = dao.getAllParkingLots().firstOrNull()
                if (existingLots.isNullOrEmpty()) {
                    // Seed Vehicles
                    dao.insertVehicle(Vehicle(1, "TN38AB1234", "Car", "Hyundai", "i20", "Polar White"))
                    dao.insertVehicle(Vehicle(2, "TN38XY6789", "Bike", "Honda", "Activa", "Matte Black"))

                    // Seed Parking Lots
                    dao.insertParkingLot(
                        ParkingLot(
                            id = 1,
                            name = "ABC Bakery Parking",
                            location = "R.S. Puram, Coimbatore",
                            totalSlots = 20,
                            availableSlots = 18,
                            pricePerHour = 30.0,
                            distanceMeters = 300,
                            hasCCTV = true,
                            isCovered = true,
                            hasEVCharging = true,
                            hasSecurity = true,
                            hasWashroom = true,
                            openingHours = "9AM–10PM",
                            rating = 4.6,
                            weekdayPrice = 30.0,
                            weekendPrice = 40.0,
                            festivalPrice = 50.0,
                            peakHoursPrice = 45.0,
                            bikeSlotsTotal = 10,
                            bikeSlotsAvailable = 8,
                            carSlotsTotal = 6,
                            carSlotsAvailable = 6,
                            suvSlotsTotal = 2,
                            suvSlotsAvailable = 2,
                            evSlotsTotal = 2,
                            evSlotsAvailable = 2
                        )
                    )

                    dao.insertParkingLot(
                        ParkingLot(
                            id = 2,
                            name = "Central Mall Premium Parking",
                            location = "Avinashi Road, Coimbatore",
                            totalSlots = 50,
                            availableSlots = 12,
                            pricePerHour = 50.0,
                            distanceMeters = 800,
                            hasCCTV = true,
                            isCovered = true,
                            hasEVCharging = true,
                            hasSecurity = true,
                            hasWashroom = true,
                            openingHours = "24×7",
                            rating = 4.2,
                            weekdayPrice = 50.0,
                            weekendPrice = 70.0,
                            festivalPrice = 90.0,
                            peakHoursPrice = 80.0,
                            bikeSlotsTotal = 20,
                            bikeSlotsAvailable = 5,
                            carSlotsTotal = 15,
                            carSlotsAvailable = 3,
                            suvSlotsTotal = 10,
                            suvSlotsAvailable = 2,
                            evSlotsTotal = 5,
                            evSlotsAvailable = 2
                        )
                    )

                    dao.insertParkingLot(
                        ParkingLot(
                            id = 3,
                            name = "Metro Station Plaza Parking",
                            location = "Gandhipuram, Coimbatore",
                            totalSlots = 100,
                            availableSlots = 82,
                            pricePerHour = 20.0,
                            distanceMeters = 1200,
                            hasCCTV = true,
                            isCovered = false,
                            hasEVCharging = false,
                            hasSecurity = true,
                            hasWashroom = true,
                            openingHours = "6AM–11PM",
                            rating = 4.0,
                            weekdayPrice = 20.0,
                            weekendPrice = 25.0,
                            festivalPrice = 30.0,
                            peakHoursPrice = 25.0,
                            bikeSlotsTotal = 50,
                            bikeSlotsAvailable = 45,
                            carSlotsTotal = 30,
                            carSlotsAvailable = 25,
                            suvSlotsTotal = 15,
                            suvSlotsAvailable = 10,
                            evSlotsTotal = 5,
                            evSlotsAvailable = 2
                        )
                    )

                    dao.insertParkingLot(
                        ParkingLot(
                            id = 4,
                            name = "Town Hall Public Grounds",
                            location = "Town Hall, Coimbatore",
                            totalSlots = 15,
                            availableSlots = 1,
                            pricePerHour = 15.0,
                            distanceMeters = 450,
                            hasCCTV = false,
                            isCovered = false,
                            hasEVCharging = false,
                            hasSecurity = false,
                            hasWashroom = false,
                            openingHours = "8AM–8PM",
                            rating = 3.5,
                            weekdayPrice = 15.0,
                            weekendPrice = 20.0,
                            festivalPrice = 25.0,
                            peakHoursPrice = 18.0,
                            bikeSlotsTotal = 10,
                            bikeSlotsAvailable = 1,
                            carSlotsTotal = 4,
                            carSlotsAvailable = 0,
                            suvSlotsTotal = 1,
                            suvSlotsAvailable = 0,
                            evSlotsTotal = 0,
                            evSlotsAvailable = 0
                        )
                    )

                    // Seed Wallet
                    dao.insertWallet(Wallet(id = 1, balance = 520.0, loyaltyPoints = 120))

                    // Seed Wallet Transactions
                    dao.insertWalletTransaction(WalletTransaction(1, "Cashback", 20.0, "Cashback reward for booking #PK-93105"))
                    dao.insertWalletTransaction(WalletTransaction(2, "Add", 500.0, "Added money via GPay UPI"))
                    dao.insertWalletTransaction(WalletTransaction(3, "Referral", 50.0, "Referral reward for inviting friend Rajesh"))

                    // Seed System Config
                    dao.insertSystemConfig(SystemConfig(id = 1, commissionPercentage = 30.0))

                    // Seed some initial partners for Admin approval demo
                    dao.insertPartner(
                        Partner(
                            id = 1,
                            name = "Karthik Raja",
                            email = "karthik@gmail.com",
                            mobileNumber = "9876543210",
                            aadhaar = "1234-5678-9012",
                            pan = "ABCDE1234F",
                            shopLicense = "LIC-RS-9921",
                            bankAccount = "SBI - 32890123891",
                            status = "Pending",
                            shopPhotoUrl = "shop_karthik",
                            parkingPhotoUrl = "parking_karthik"
                        )
                    )
                    dao.insertPartner(
                        Partner(
                            id = 2,
                            name = "Suresh Kumar",
                            email = "suresh@gmail.com",
                            mobileNumber = "9988776655",
                            aadhaar = "9876-5432-1098",
                            pan = "XYZWP5678Q",
                            shopLicense = "LIC-RS-1044",
                            bankAccount = "HDFC - 50100921822",
                            status = "Approved",
                            shopPhotoUrl = "shop_suresh",
                            parkingPhotoUrl = "parking_suresh"
                        )
                    )
                    
                    // Seed a complete and a cancelled booking for history
                    dao.insertBooking(
                        Booking(
                            id = 1,
                            bookingId = "PK-12891",
                            parkingLotId = 1,
                            parkingLotName = "ABC Bakery Parking",
                            vehicleId = 1,
                            vehicleNumber = "TN38AB1234",
                            vehicleType = "Car",
                            date = "2026-07-01",
                            time = "10:00 AM",
                            durationHours = 2,
                            totalPrice = 60.0,
                            qrCodeString = "PK-12891_VALID",
                            status = "Completed",
                            checkInTime = System.currentTimeMillis() - 172800000,
                            checkOutTime = System.currentTimeMillis() - 172800000 + 7200000,
                            finalBill = 60.0,
                            cleanlinessRating = 5,
                            securityRating = 4,
                            easeOfEntryRating = 5,
                            comments = "Very easy to park, security was supportive."
                        )
                    )
                }
            }
        }
    }
}

class ParkEasyRepository(private val dao: ParkEasyDao) {
    val allVehicles: Flow<List<Vehicle>> = dao.getAllVehicles()
    val allParkingLots: Flow<List<ParkingLot>> = dao.getAllParkingLots()
    val allBookings: Flow<List<Booking>> = dao.getAllBookings()
    val allPartners: Flow<List<Partner>> = dao.getAllPartners()
    val wallet: Flow<Wallet?> = dao.getWalletFlow()
    val walletTransactions: Flow<List<WalletTransaction>> = dao.getWalletTransactions()
    val systemConfig: Flow<SystemConfig?> = dao.getSystemConfigFlow()

    suspend fun insertVehicle(vehicle: Vehicle) = dao.insertVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = dao.deleteVehicle(vehicle)

    suspend fun getParkingLotById(id: Long): ParkingLot? = dao.getParkingLotById(id)
    suspend fun insertParkingLot(parkingLot: ParkingLot) = dao.insertParkingLot(parkingLot)
    suspend fun updateParkingLot(parkingLot: ParkingLot) = dao.updateParkingLot(parkingLot)
    suspend fun deleteParkingLot(parkingLot: ParkingLot) = dao.deleteParkingLot(parkingLot)

    suspend fun getBookingById(id: Long): Booking? = dao.getBookingById(id)
    suspend fun insertBooking(booking: Booking): Long = dao.insertBooking(booking)
    suspend fun updateBooking(booking: Booking) = dao.updateBooking(booking)

    suspend fun insertPartner(partner: Partner) = dao.insertPartner(partner)
    suspend fun updatePartner(partner: Partner) = dao.updatePartner(partner)

    suspend fun updateWallet(wallet: Wallet) = dao.insertWallet(wallet)
    suspend fun insertWalletTransaction(transaction: WalletTransaction) = dao.insertWalletTransaction(transaction)

    suspend fun updateSystemConfig(config: SystemConfig) = dao.insertSystemConfig(config)
    suspend fun getSystemConfig(): SystemConfig? = dao.getSystemConfig()
}
