package com.example.menu_app.viewModel

import android.util.Log
import androidx.lifecycle.*
import com.example.menu_app.database.basket.CartEntity
import com.example.menu_app.database.basket.CartRepository
import com.example.menu_app.database.dishes.DishRepository
import com.example.menu_app.database.dishes.DishesEntity
import com.example.menu_app.database.orders.CartList
import com.example.menu_app.database.orders.OrdersEntity
import com.example.menu_app.database.orders.OrdersRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

class mainViewModel (private val cartRepo: CartRepository, private val dishRepo: DishRepository, private val ordersRepo: OrdersRepository) : ViewModel() {

    //Define LiveData properties
    // For dish page
    private val dishData = MutableLiveData<DishesEntity>()

    //For order page
    private val orderData = MutableLiveData<OrdersEntity>()

    // For basket and main fragment
    val textItemCount: LiveData<Int> = MutableLiveData()
    val totalBasketPrice: LiveData<Double> = MutableLiveData()

    val filteredDishes: LiveData<List<DishesEntity>> = MutableLiveData()
    val isCartEmpty: LiveData<Boolean> = MutableLiveData()

    val billName: LiveData<String> = MutableLiveData()

    // For all edit modes
    private val _isEditModeEnabled = MutableLiveData<Boolean>()
    val isEditModeEnabled: LiveData<Boolean> = _isEditModeEnabled

    // For when the main database has been changed
    private val _changesMade = MutableLiveData<Boolean>()
    val changesMade: LiveData<Boolean> = _changesMade

    private val _databaseChanged = MutableLiveData<Boolean>()
    val databaseChanged: LiveData<Boolean> = _databaseChanged

    private val _mainChanged = MutableLiveData<Boolean>()
    val mainChanged: LiveData<Boolean> = _mainChanged

    // For when the order history database is changed
    private val _ordersFragChanged = MutableLiveData<Boolean>()
    val ordersFragChanged: LiveData<Boolean> = _ordersFragChanged

    // When the app is initialised
    init {
        //Immediately clears the cart when app is opened
        viewModelScope.launch {
            cartRepo.clearCart()
            (isCartEmpty as MutableLiveData).value = true
            (isEditModeEnabled as MutableLiveData).value = false
            (changesMade as MutableLiveData).value = false
            (databaseChanged as MutableLiveData).value = false
            (mainChanged as MutableLiveData).value = false
            //getOrders()
        }
    }

    suspend fun addToCart(position: DishesEntity){
        val existingItem = cartRepo.getCartItemByName(position.dishEnglishName)
        if (existingItem == null){
            val cartEntity = CartEntity(
                name = position.dishEnglishName,
                price = position.dishPrice,
                quantity = 1,
                notes = ""
            )
            cartRepo.insertCartItem(cartEntity)
        }else{
            existingItem.quantity++
            cartRepo.updateCartItem(existingItem)
        }

        (isCartEmpty as MutableLiveData).value = false
        updateItemCount()
        updateTotalBasketPrice()
    }

    suspend fun addNoteToCart(dish: DishesEntity, note: String){
        Log.d("MainViewModel", "addNoteToCart: $note")
        val existingItem = cartRepo.getCartItemByName(dish.dishEnglishName)
        if (existingItem == null){
            val cartEntity = CartEntity(
                name = dish.dishEnglishName,
                price = dish.dishPrice,
                quantity = 1,
                notes = note
            )
            cartRepo.insertCartItem(cartEntity)
        } else {
            existingItem.quantity++
            existingItem.notes = note
            cartRepo.updateCartItem(existingItem)
        }
        (isCartEmpty as MutableLiveData).value = false
        updateItemCount()
        updateTotalBasketPrice()
    }

    private suspend fun isCartEmpty(){
        viewModelScope.launch {
            (isCartEmpty as MutableLiveData).value = cartRepo.getAllCartItems().isEmpty()
        }
    }

    fun filterDishes(dishes: List<DishesEntity>, newText: String): List<DishesEntity>{
        // Filter dishes based on search text
        return dishes.filter { dish ->
            dish.dishEnglishName.contains(newText, ignoreCase = true) ||
                    dish.dishId.toString().contains(newText, ignoreCase = true)
        }
    }

    // Define a function to update the state of your UI
    private suspend fun updateItemCount() {
        val totalItems = cartRepo.getAllCartItems().sumOf { it.quantity }
        Log.d("mainViewModel", "updateItemCount: $totalItems")
        (textItemCount as MutableLiveData).value = totalItems
        isCartEmpty()
    }

    private suspend fun updateTotalBasketPrice() {
        val totalPrice = cartRepo.getTotalPrice()
        (totalBasketPrice as MutableLiveData).value = totalPrice
    }

    fun updateCart(){
        viewModelScope.launch {
            updateItemCount()
            updateTotalBasketPrice()
        }
    }

    fun clearCart(){
        viewModelScope.launch {
            cartRepo.clearCart()
            (isCartEmpty as MutableLiveData).value = true
            updateCart()
            setBillName("")
        }
    }

    fun setBillName(name: String){
        (billName as MutableLiveData).value = name
    }

    fun setEditMode(mode: Boolean) {
        _isEditModeEnabled.value = mode
    }

    fun updateIndividualTotalPrice() {
        viewModelScope.launch {
            val cartItems = cartRepo.getAllCartItems()
            val totalPrice = cartItems.sumOf { it.price * it.quantity }
            (totalBasketPrice as MutableLiveData).value = totalPrice
        }
    }

    fun triggerRefresh() {
        _changesMade.value = true
        Log.d("mainViewModel", "Trigger refresh: Refresh triggering")
    }

    fun refreshDone(fragment: String){
        when (fragment){
            "database" -> _databaseChanged.value = true
            "main" -> _mainChanged.value = true

            // The bit for orders history
            "orderHistory" -> _ordersFragChanged.value = false
        }
        if (_databaseChanged.value == true && _mainChanged.value == true){
            _changesMade.value = false
            Log.d("mainViewModel", "Refresh done: changesMade set to false")
        }
    }

    // Generally for the dish page //////////////////////////////////////////
    fun setDishesData(dishes: DishesEntity){
        dishData.value = dishes
    }

    fun getDishesData(): LiveData<DishesEntity> {
        return dishData
    }

    suspend fun saveChanges(
        dishIdNo: Long,
        dishNumber: String,
        dishName: String,
        dishCnName: String,
        staffName: String,
        price: Double
    ){
        try {
            val existingDish = dishRepo.getDishByID(dishIdNo)
            existingDish?.apply {
                dishId = dishNumber
                dishEnglishName = dishName
                dishChineseName = dishCnName
                dishStaffName = staffName
                dishPrice = price
            }
            existingDish?.let { dishRepo.updateDish(it) }
            existingDish?.let { setDishesData(it) }
//            dishRepo.updateDish(updatedDish)
//            setDishesData(updatedDish)
            Log.d("Updated dish", existingDish.toString())
            Log.d("mainViewModel", changesMade.value.toString())
            _changesMade.value = true
        }catch (e: Exception){
            Log.d("Error updating dish", e.message.toString())
            _changesMade.value = false
        }finally {
            setEditMode(false)
            Log.d("mainViewModel", "saveChanges: finally - edit mode off")
            Log.d("mainViewModel", "Changes made: "+changesMade.value.toString())
        }
    }

    suspend fun deleteDish(dish: DishesEntity){
        try {
            dishRepo.delete(dish)
        }catch (e: Exception){
            Log.d("Error deleting dish", e.message.toString())
            _changesMade.value = false
        }
    }


    fun saveOrder(number:Int, billName: String, orderDate: LocalDate, orderTime: LocalTime, cart: CartList){
        Log.d("mainViewModel", "saving order")
        val totalPrice = totalBasketPrice.value ?: 0.0
        viewModelScope.launch {
            val order = OrdersEntity(
                orderNumber = number,
                orderName = billName,
                date = orderDate,
                time = orderTime,
                orders = cart,
                price = totalPrice
            )
            ordersRepo.insertOrder(order)
            //cartRepo.clearCart()
            _ordersFragChanged.value = true
            Log.d("mainViewModel", "Order is $order")
        }
        Log.d("mainViewModel", "Order saved")
    }

    // For order page
    fun setOrderData(order: OrdersEntity){
        orderData.value = order
    }

    fun getOrderData(): LiveData<OrdersEntity> {
        return orderData
    }
}