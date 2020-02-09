package com.sarahehabm.restaurants.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sarahehabm.restaurants.repository.RestaurantsRepository
import com.sarahehabm.restaurants.repository.getNetworkService
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MapViewModelTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private var restaurantsRepository = RestaurantsRepository(getNetworkService())
    private var mapViewModel = MapViewModel(restaurantsRepository)

    @Test
    fun viewModel_testNotNull() {
        Assert.assertThat(mapViewModel.getRestaurants(), notNullValue())
    }

    @Test
    fun viewModel_emptyList() {
        mapViewModel.setRestaurants(arrayListOf())

        Assert.assertThat(mapViewModel.getRestaurants().value?.isEmpty(), `is`(true))
    }
}