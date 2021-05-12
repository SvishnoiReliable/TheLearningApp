package com.learn.repository

import com.learn.model.response.helpCenterCategories.HelpCenterCategoriesResponse
import com.learn.network.ApiRestService
import com.learn.network.SafeApiRequest


class HelpCenterRepository(private val api: ApiRestService) : SafeApiRequest() {
    suspend fun getHelpCenter(): HelpCenterCategoriesResponse {
        return apiRequest { api.getHelpCenter() }
    }
}
