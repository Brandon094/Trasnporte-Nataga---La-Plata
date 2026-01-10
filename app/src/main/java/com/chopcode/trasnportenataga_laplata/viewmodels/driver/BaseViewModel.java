// BaseViewModel.java
package com.chopcode.trasnportenataga_laplata.viewmodels.driver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

public abstract class BaseViewModel extends ViewModel {
    protected static final String TAG = "BaseViewModel";
    protected final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    protected final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    public LiveData<String> getErrorLiveData() { return errorLiveData; }
    public LiveData<Boolean> getLoadingLiveData() { return loadingLiveData; }

    protected void setLoading(boolean loading) {
        loadingLiveData.postValue(loading);
    }

    protected void setError(String error) {
        Log.e(TAG, "Error: " + error);
        errorLiveData.postValue(error);
    }

    // ✅ AGREGAR ESTE MÉTODO PROTEGIDO
    protected void registrarEventoAnalitico(String evento, String conductor, Integer cantidad) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("viewmodel", this.getClass().getSimpleName());
            params.put("conductor_id", com.chopcode.trasnportenataga_laplata.config.MyApp.getCurrentUserId());

            if (conductor != null) params.put("conductor", conductor);
            if (cantidad != null) params.put("cantidad", cantidad);
            params.put("timestamp", System.currentTimeMillis());

            com.chopcode.trasnportenataga_laplata.config.MyApp.logEvent("vm_" + evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel " + getClass().getSimpleName() + " destruido");
    }
}