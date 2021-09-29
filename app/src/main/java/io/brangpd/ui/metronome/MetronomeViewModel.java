package io.brangpd.ui.metronome;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MetronomeViewModel extends ViewModel {
    private final MutableLiveData<Integer> mBpm;

    public MetronomeViewModel() {
        mBpm = new MutableLiveData<>(60);
    }

    public LiveData<Integer> getBpm() {
        return mBpm;
    }

    public void setBpm(int v) {
        mBpm.setValue(v);
    }
}