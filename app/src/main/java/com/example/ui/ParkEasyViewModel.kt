package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ParkEasyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ParkEasyRepository

    // Global Roles: "Customer", "Partner", "Admin"
    val userRole = MutableStateFlow("Customer")

    // Navigation Screens
    // Customer: "Home", "Details", "BookingFlow", "Confirmation", "Wallet", "History", "Timer", "Review", "Favorites", "Notifications", "Pass"
    val customerScreen = MutableStateFlow("Home")
    // Partner: "Dashboard", "LotForm", "LotDetail", "SlotManage", "Earnings", "Reviews"
    val partnerScreen = MutableStateFlow("Dashboard")
    // Admin: "Dashboard", "Approvals", "Lots", "Users", "Config"
    val adminScreen = MutableStateFlow("Dashboard")

    // Database flows wrapped in StateFlows
    val vehicles: StateFlow<List<Vehicle>>
    val parkingLots: StateFlow<List<ParkingLot>>
    val bookings: StateFlow<List<Booking>>
    val partners: StateFlow<List<Partner>>
    val wallet: StateFlow<Wallet?>
    val walletTransactions: StateFlow<List<WalletTransaction>>
    val systemConfig: StateFlow<SystemConfig?>

    // Interactive States
    val selectedLot = MutableStateFlow<ParkingLot?>(null)
    val activeBooking = MutableStateFlow<Booking?>(null)
    val confirmedBooking = MutableStateFlow<Booking?>(null)
    val searchFilters = MutableStateFlow(MapFilters())
    val searchQuery = MutableStateFlow("")
    val bookingHistoryFilter = MutableStateFlow("All") // All, Upcoming, Completed, Cancelled

    // AI Prediction States
    val aiPrediction = MutableStateFlow<AIPrediction?>(null)
    val isPredicting = MutableStateFlow(false)

    // User Profile Settings
    val profileName = MutableStateFlow("Bro")
    val profileMobile = MutableStateFlow("+91 98765 43210")
    val profileEmail = MutableStateFlow("bro@parkeasy.com")
    val isDarkTheme = MutableStateFlow(false)
    val hasMonthlyPass = MutableStateFlow(false)
    val monthlyPassExpiry = MutableStateFlow("")

    // Active screen navigation stacks (simple stack)
    private val customerScreenStack = mutableListOf<String>()

    init {
        val database = ParkEasyDatabase.getDatabase(application)
        repository = ParkEasyRepository(database.dao())

        vehicles = repository.allVehicles.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        parkingLots = repository.allParkingLots.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        bookings = repository.allBookings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        partners = repository.allPartners.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        wallet = repository.wallet.stateIn(viewModelScope, SharingStarted.Lazily, Wallet())
        walletTransactions = repository.walletTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        systemConfig = repository.systemConfig.stateIn(viewModelScope, SharingStarted.Lazily, SystemConfig())
    }

    // Navigation Helper
    fun navigateToCustomer(screen: String) {
        customerScreenStack.add(customerScreen.value)
        customerScreen.value = screen
    }

    fun navigateBackCustomer() {
        if (customerScreenStack.isNotEmpty()) {
            customerScreen.value = customerScreenStack.removeAt(customerScreenStack.size - 1)
        } else {
            customerScreen.value = "Home"
        }
    }

    // --- Customer Functions ---

    fun toggleFavorite(lot: ParkingLot) {
        viewModelScope.launch {
            repository.updateParkingLot(lot.copy(isSavedAsFavorite = !lot.isSavedAsFavorite))
            if (selectedLot.value?.id == lot.id) {
                selectedLot.value = lot.copy(isSavedAsFavorite = !lot.isSavedAsFavorite)
            }
        }
    }

    fun registerVehicle(number: String, type: String, brand: String, model: String, color: String) {
        viewModelScope.launch {
            repository.insertVehicle(Vehicle(number = number, type = type, brand = brand, model = model, color = color))
        }
    }

    fun removeVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }

    fun addMoneyToWallet(amount: Double) {
        viewModelScope.launch {
            val currentWallet = wallet.value ?: Wallet()
            repository.updateWallet(currentWallet.copy(balance = currentWallet.balance + amount))
            repository.insertWalletTransaction(
                WalletTransaction(type = "Add", amount = amount, description = "Added money via GPay UPI")
            )
        }
    }

    fun buyMonthlyPass() {
        viewModelScope.launch {
            val currentWallet = wallet.value ?: Wallet()
            if (currentWallet.balance >= 2500) {
                repository.updateWallet(currentWallet.copy(balance = currentWallet.balance - 2500))
                repository.insertWalletTransaction(
                    WalletTransaction(type = "Spend", amount = 2500.0, description = "Purchased Office Monthly Pass")
                )
                hasMonthlyPass.value = true
                monthlyPassExpiry.value = "August 4, 2026"
            }
        }
    }

    fun claimReferralReward() {
        viewModelScope.launch {
            val currentWallet = wallet.value ?: Wallet()
            repository.updateWallet(currentWallet.copy(balance = currentWallet.balance + 50.0))
            repository.insertWalletTransaction(
                WalletTransaction(type = "Referral", amount = 50.0, description = "Referral bonus code: PE50REF")
            )
        }
    }

    fun applyCoupon(code: String): Boolean {
        if (code.uppercase() == "PARKEASY20") {
            viewModelScope.launch {
                val currentWallet = wallet.value ?: Wallet()
                repository.updateWallet(currentWallet.copy(balance = currentWallet.balance + 20.0))
                repository.insertWalletTransaction(
                    WalletTransaction(type = "Cashback", amount = 20.0, description = "Coupon PARKEASY20 applied")
                )
            }
            return true
        }
        return false
    }

    // AI Prediction triggers
    fun triggerAiPrediction(lot: ParkingLot, day: String, time: String) {
        viewModelScope.launch {
            isPredicting.value = true
            aiPrediction.value = AIParkingPredictor.predictParkingDemand(
                lot.name, lot.location, day, time, lot.pricePerHour
            )
            isPredicting.value = false
        }
    }

    // Booking actions
    fun createBooking(
        lot: ParkingLot,
        vehicle: Vehicle,
        date: String,
        time: String,
        duration: Int,
        isEvCharging: Boolean
    ) {
        viewModelScope.launch {
            val pricePerHr = lot.pricePerHour
            val extraCost = if (isEvCharging) 20.0 else 0.0
            val total = (pricePerHr * duration) + extraCost
            
            val bookingId = "PK-${(10000..99999).random()}"
            val booking = Booking(
                bookingId = bookingId,
                parkingLotId = lot.id,
                parkingLotName = lot.name,
                vehicleId = vehicle.id,
                vehicleNumber = vehicle.number,
                vehicleType = vehicle.type,
                date = date,
                time = time,
                durationHours = duration,
                totalPrice = total,
                qrCodeString = "${bookingId}_VALID",
                status = "Upcoming",
                isEvChargingReserved = isEvCharging
            )
            val id = repository.insertBooking(booking)
            val savedBooking = booking.copy(id = id)
            confirmedBooking.value = savedBooking

            // Decrement available slots locally
            repository.updateParkingLot(lot.copy(
                availableSlots = (lot.availableSlots - 1).coerceAtLeast(0),
                carSlotsAvailable = if (vehicle.type == "Car") (lot.carSlotsAvailable - 1).coerceAtLeast(0) else lot.carSlotsAvailable,
                bikeSlotsAvailable = if (vehicle.type == "Bike") (lot.bikeSlotsAvailable - 1).coerceAtLeast(0) else lot.bikeSlotsAvailable,
                suvSlotsAvailable = if (vehicle.type == "SUV") (lot.suvSlotsAvailable - 1).coerceAtLeast(0) else lot.suvSlotsAvailable,
                evSlotsAvailable = if (vehicle.type == "EV") (lot.evSlotsAvailable - 1).coerceAtLeast(0) else lot.evSlotsAvailable
            ))

            navigateToCustomer("Confirmation")
        }
    }

    fun checkInBooking(booking: Booking) {
        viewModelScope.launch {
            val updated = booking.copy(
                status = "CheckedIn",
                checkInTime = System.currentTimeMillis()
            )
            repository.updateBooking(updated)
            activeBooking.value = updated
            navigateToCustomer("Timer")
        }
    }

    fun extendParking(booking: Booking, hours: Int) {
        viewModelScope.launch {
            val additionalPrice = 30.0 * hours
            val currentWallet = wallet.value ?: Wallet()
            
            if (currentWallet.balance >= additionalPrice) {
                repository.updateWallet(currentWallet.copy(balance = currentWallet.balance - additionalPrice))
                repository.insertWalletTransaction(
                    WalletTransaction(type = "Spend", amount = additionalPrice, description = "Extended parking booking #${booking.bookingId}")
                )

                val updated = booking.copy(
                    durationHours = booking.durationHours + hours,
                    totalPrice = booking.totalPrice + additionalPrice
                )
                repository.updateBooking(updated)
                activeBooking.value = updated
            }
        }
    }

    fun checkOutBooking(booking: Booking, overtimeHours: Int = 0) {
        viewModelScope.launch {
            val overCharge = overtimeHours * 50.0 // 50 per hr overtime penalty
            val finalAmount = booking.totalPrice + overCharge

            val currentWallet = wallet.value ?: Wallet()
            val finalBalance = (currentWallet.balance - finalAmount).coerceAtLeast(0.0)
            
            repository.updateWallet(currentWallet.copy(
                balance = finalBalance,
                loyaltyPoints = currentWallet.loyaltyPoints + 15
            ))

            if (finalAmount > 0) {
                repository.insertWalletTransaction(
                    WalletTransaction(
                        type = "Spend",
                        amount = finalAmount,
                        description = "Completed parking checkout for #${booking.bookingId}"
                    )
                )
            }

            val updated = booking.copy(
                status = "Completed",
                checkOutTime = System.currentTimeMillis(),
                finalBill = finalAmount
            )
            repository.updateBooking(updated)
            
            // Release lot slot
            val lot = repository.getParkingLotById(booking.parkingLotId)
            if (lot != null) {
                repository.updateParkingLot(lot.copy(
                    availableSlots = (lot.availableSlots + 1).coerceAtMost(lot.totalSlots),
                    carSlotsAvailable = if (booking.vehicleType == "Car") (lot.carSlotsAvailable + 1).coerceAtMost(lot.carSlotsTotal) else lot.carSlotsAvailable,
                    bikeSlotsAvailable = if (booking.vehicleType == "Bike") (lot.bikeSlotsAvailable + 1).coerceAtMost(lot.bikeSlotsTotal) else lot.bikeSlotsAvailable,
                    suvSlotsAvailable = if (booking.vehicleType == "SUV") (lot.suvSlotsAvailable + 1).coerceAtMost(lot.suvSlotsTotal) else lot.suvSlotsAvailable,
                    evSlotsAvailable = if (booking.vehicleType == "EV") (lot.evSlotsAvailable + 1).coerceAtMost(lot.evSlotsTotal) else lot.evSlotsAvailable
                ))
            }

            activeBooking.value = null
            navigateToCustomer("Review")
        }
    }

    fun submitReview(bookingId: Long, cleanliness: Int, security: Int, ease: Int, comment: String) {
        viewModelScope.launch {
            val booking = repository.getBookingById(bookingId)
            if (booking != null) {
                val updated = booking.copy(
                    cleanlinessRating = cleanliness,
                    securityRating = security,
                    easeOfEntryRating = ease,
                    comments = comment
                )
                repository.updateBooking(updated)
                
                // Recalculate average rating for that lot
                val avgRating = (cleanliness + security + ease) / 3.0
                val lot = repository.getParkingLotById(booking.parkingLotId)
                if (lot != null) {
                    val finalRating = ((lot.rating * 4) + avgRating) / 5.0
                    repository.updateParkingLot(lot.copy(rating = Math.round(finalRating * 10.0) / 10.0))
                }
            }
            navigateToCustomer("Home")
        }
    }

    // --- Partner Functions ---

    fun registerPartnerProfile(
        name: String,
        email: String,
        mobile: String,
        aadhaar: String,
        pan: String,
        shopLicense: String,
        bankAccount: String
    ) {
        viewModelScope.launch {
            repository.insertPartner(
                Partner(
                    name = name,
                    email = email,
                    mobileNumber = mobile,
                    aadhaar = aadhaar,
                    pan = pan,
                    shopLicense = shopLicense,
                    bankAccount = bankAccount,
                    status = "Pending"
                )
            )
            partnerScreen.value = "Dashboard"
        }
    }

    fun addOrUpdatePartnerLot(lot: ParkingLot) {
        viewModelScope.launch {
            if (lot.id == 0L) {
                repository.insertParkingLot(lot)
            } else {
                repository.updateParkingLot(lot)
            }
            partnerScreen.value = "Dashboard"
        }
    }

    fun deletePartnerLot(lot: ParkingLot) {
        viewModelScope.launch {
            repository.deleteParkingLot(lot)
        }
    }

    // --- Admin Functions ---

    fun approvePartner(partner: Partner) {
        viewModelScope.launch {
            repository.updatePartner(partner.copy(status = "Approved"))
            
            // Auto create a default parking lot for the approved partner
            repository.insertParkingLot(
                ParkingLot(
                    name = "${partner.name}'s Premium Hub",
                    location = "Town Center, Coimbatore",
                    totalSlots = 30,
                    availableSlots = 30,
                    pricePerHour = 40.0,
                    distanceMeters = 500,
                    hasCCTV = true,
                    isCovered = true,
                    hasEVCharging = false,
                    hasSecurity = true,
                    hasWashroom = true,
                    openingHours = "8AM-11PM",
                    rating = 4.8,
                    weekdayPrice = 40.0,
                    weekendPrice = 50.0,
                    festivalPrice = 60.0,
                    peakHoursPrice = 55.0,
                    isApproved = true,
                    bikeSlotsTotal = 15,
                    bikeSlotsAvailable = 15,
                    carSlotsTotal = 10,
                    carSlotsAvailable = 10,
                    suvSlotsTotal = 3,
                    suvSlotsAvailable = 3,
                    evSlotsTotal = 2,
                    evSlotsAvailable = 2
                )
            )
        }
    }

    fun rejectPartner(partner: Partner) {
        viewModelScope.launch {
            repository.updatePartner(partner.copy(status = "Rejected"))
        }
    }

    fun suspendPartner(partner: Partner) {
        viewModelScope.launch {
            repository.updatePartner(partner.copy(status = "Suspended"))
        }
    }

    fun setPlatformCommission(percentage: Double) {
        viewModelScope.launch {
            val config = repository.getSystemConfig() ?: SystemConfig()
            repository.updateSystemConfig(config.copy(commissionPercentage = percentage))
        }
    }
}

// Support Structs
data class MapFilters(
    val cheapest: Boolean = false,
    val nearest: Boolean = false,
    val covered: Boolean = false,
    val cctv: Boolean = false,
    val evCharging: Boolean = false,
    val security: Boolean = false,
    val washroom: Boolean = false
)
