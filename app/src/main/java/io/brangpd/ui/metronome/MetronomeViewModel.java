package io.brangpd.ui.metronome;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MetronomeViewModel extends ViewModel {
    private final MutableLiveData<Integer> bpm;

    public MetronomeViewModel() {
        bpm = new MutableLiveData<>(MetronomeFragment.kInitBpm);
    }

    @NonNull
    public LiveData<Integer> getBpm() {
        return bpm;
    }

    public void setBpm(int v) {
        v = Math.min(v, MetronomeFragment.kMaxBpm);
        v = Math.max(v, MetronomeFragment.kMinBpm);
        bpm.setValue(v);
    }
}