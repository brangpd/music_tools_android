package io.brangpd.ui.midipiano;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import org.billthefarmer.mididriver.MidiDriver;

import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

import io.brangpd.R;
import io.brangpd.databinding.MidiPianoFragmentBinding;

public class MidiPianoFragment extends Fragment {
    private static final String TAG = MidiPianoFragment.class.toString();

    private MidiPianoViewModel mViewModel;
    private int mWhiteKeyWidth = 100;
    private int mWhiteKeyHeight = 450;
    private TextView mTextViewMidiNote;
    private Spinner mSpinnerMidiKeySize;
    private MidiPianoFragmentBinding mBinding;
    private Spinner mSpinnerMidiKeyTransposition;
    private static final int kMinDelta = -11;
    private static final int kMaxDelta = 11;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MidiDriver.getInstance().start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MidiDriver.getInstance().stop();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(MidiPianoViewModel.class);
        View view = inflater.inflate(R.layout.midi_piano_fragment, container, false);
        mBinding = MidiPianoFragmentBinding.bind(view);
        LinearLayout linearLayoutScrollViewMidiKeyboards = mBinding.layoutMidiPianoKeyboards.linearLayoutScrollViewMidiKeyboards;
        mTextViewMidiNote = mBinding.textViewMidiNote;
        mSpinnerMidiKeySize = mBinding.layoutMidiPianoQuickSettings.spinnerMidiKeySize;
        mSpinnerMidiKeyTransposition = mBinding.layoutMidiPianoQuickSettings.spinnerMidiKeyTransposition;
        Button buttonMidiKeyTranspositionPlus = mBinding.layoutMidiPianoQuickSettings.buttonMidiKeyTranspositionPlus;
        Button buttonMidiKeyTranspositionMinus = mBinding.layoutMidiPianoQuickSettings.buttonMidiKeyTranspositionMinus;
        Button buttonMidiKeyTranspositionZero = mBinding.layoutMidiPianoQuickSettings.buttonMidiKeyTranspositionZero;

        // 移调部分UI及行为
        // 移调列表
        ArrayAdapter<MidiKeyTransposition> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new Vector<MidiKeyTransposition>() {{
                    for (int i = kMinDelta; i <= kMaxDelta; ++i) {
                        add(new MidiKeyTransposition(i));
                    }
                }});
        mSpinnerMidiKeyTransposition.setAdapter(adapter);
        mSpinnerMidiKeyTransposition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Object selectedItem = adapterView.getSelectedItem();
                if (selectedItem instanceof MidiKeyTransposition) {
                    int delta = ((MidiKeyTransposition) selectedItem).getDelta();
                    mViewModel.setTransposition(delta);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        buttonMidiKeyTranspositionPlus.setOnClickListener(view1 -> {
            int cur = Objects.requireNonNull(mViewModel.getTransposition().getValue());
            if (cur < kMaxDelta) {
                mViewModel.setTransposition(cur + 1);
            }
        });
        buttonMidiKeyTranspositionMinus.setOnClickListener(view1 -> {
            int cur = Objects.requireNonNull(mViewModel.getTransposition().getValue());
            if (cur > kMinDelta) {
                mViewModel.setTransposition(cur - 1);
            }
        });
        buttonMidiKeyTranspositionZero.setOnClickListener(view1 -> mViewModel.setTransposition(0));
        mViewModel.getTransposition().observe(getViewLifecycleOwner(), integer -> {
            int index = integer - kMinDelta;
            mSpinnerMidiKeyTransposition.setSelection(index);
        });
        // 一开始列表会显示为第一个加入的项（-11），手动调用一次设置
        mViewModel.setTransposition(Objects.requireNonNull(mViewModel.getTransposition().getValue()));
        mSpinnerMidiKeySize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mWhiteKeyWidth = Integer.parseInt(adapterView.getSelectedItem().toString());
                buildKeys(linearLayoutScrollViewMidiKeyboards);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        buildKeys(linearLayoutScrollViewMidiKeyboards);

        return view;
    }

    private static class MidiKeyTransposition {
        private final int kDelta;
        private final String kString;

        public MidiKeyTransposition(int kDelta) {
            this.kDelta = kDelta;
            kString = String.format(Locale.getDefault(), "%+d %s", kDelta, pitchNameString(60 + kDelta, false));
        }

        public int getDelta() {
            return kDelta;
        }

        @NonNull
        @Override
        public String toString() {
            return kString;
        }
    }

    private void buildKeys(LinearLayout linearLayout) {
        for (int childIndex = 0; childIndex < linearLayout.getChildCount(); ++childIndex) {
            HorizontalScrollView scrollView = (HorizontalScrollView) linearLayout.getChildAt(childIndex);
            RelativeLayout relativeLayout = (RelativeLayout) scrollView.getChildAt(0);
            relativeLayout.removeAllViews();
            RelativeLayout.LayoutParams params;

            // 底部添加一点空隙方便拖拽
            Space space = new Space(getContext());
            params = new RelativeLayout.LayoutParams(52 * mWhiteKeyWidth, mWhiteKeyHeight + 100);
            params.topMargin = 0;
            params.leftMargin = 0;
            space.setLayoutParams(params);
            relativeLayout.addView(space);

            OnMidiKeyboardListener onMidiKeyboardListener = new OnMidiKeyboardListener() {
                @Override
                public void onTouch(int midiCode, int velocity) {
                    int delta = Objects.requireNonNull(mViewModel.getTransposition().getValue());
                    midiCode += delta;
                    Log.d(TAG, String.format("onCreateView Touch: %d %d", midiCode, velocity));
                    mTextViewMidiNote.setText(pitchNameString(midiCode));
                    // Construct a note ON message for the middle C at maximum velocity on channel 1:
                    byte[] event = new byte[3];
                    event[0] = (byte) 0x90;  // 0x90 = note On, 0x00 = channel 1
                    event[1] = (byte) midiCode;  // 0x3C = middle C
                    event[2] = (byte) velocity;  // 0x7F = the maximum velocity (127)

                    // Send the MIDI event to the synthesizer.
                    MidiDriver.getInstance().write(event);
                }

                @Override
                public void onRelease(int midiCode, int velocity) {
                    int delta = Objects.requireNonNull(mViewModel.getTransposition().getValue());
                    midiCode += delta;
                    Log.d(TAG, String.format("onCreateView Release: %d", midiCode));
                    // Construct a note OFF message for the middle C at minimum velocity on channel 1:
                    byte[] event = new byte[3];
                    event[0] = (byte) 0x80;  // 0x80 = note Off, 0x00 = channel 1
                    event[1] = (byte) midiCode;  // 0x3C = middle C
                    event[2] = (byte) velocity;  // 0x00 = the minimum velocity (0)

                    // Send the MIDI event to the synthesizer.
                    MidiDriver.getInstance().write(event);
                }
            };
            // 钢琴第一个键A0，左边省去20个MIDI键，即12个白键
            int leftMarginOffset = -12 * mWhiteKeyWidth;
            L_octave_loop:
            for (int octave = 0; ; ++octave) {
                for (int i = 0; i < 12; ++i) {
                    int midiCode = octave * 12 + i;
                    if (midiCode < 21 /*A0*/) {
                        continue;
                    }
                    if (midiCode > 108 /*C8*/) {
                        break L_octave_loop;
                    }
                    MidiKeyboardButton midiKeyboardButton = new MidiKeyboardButton(
                            new ContextThemeWrapper(
                                    getContext(), R.style.Widget_MaterialComponents_Button_OutlinedButton),
                            midiCode);
                    midiKeyboardButton.setOnMidiKeyboardListener(onMidiKeyboardListener);
                    if (isBlackKey(midiCode)) {
                        // 放置黑键
                        midiKeyboardButton.setStrokeColor(ColorStateList.valueOf(Color.BLACK));
                        params = new RelativeLayout.LayoutParams(
                                blackKeyWidth(mWhiteKeyWidth), blackKeyHeight(mWhiteKeyHeight));
                        params.topMargin = 0;

                        // 算当前黑键在当前八度内的位置：前2个黑键和前3个白键分5格，后3个黑键和后4个白键分7格
                        if (midiCode == 22 /*A#0*/) {
                            // 最左边一个黑键，为了美观居中，直接跟两个白键平分
                            params.leftMargin = (int) Math.ceil(mWhiteKeyWidth * 2 / 3.0);
                        } else {
                            double localBlackLeftOffset;
                            if (i <= 3) {
                                localBlackLeftOffset = mWhiteKeyWidth * 3 * i / 5.0;
                            } else {
                                localBlackLeftOffset = mWhiteKeyWidth * 3 + mWhiteKeyWidth * 4 * (i - 5) / 7.0;
                            }
                            params.leftMargin = leftMarginOffset + octave * 7 * mWhiteKeyWidth + (int) Math.round(localBlackLeftOffset);
                        }

                        Log.d(TAG, String.format("Black key %d left margin: %d", midiCode, params.leftMargin));

                        midiKeyboardButton.setLayoutParams(params);
                    } else {
                        // 放置白键
                        midiKeyboardButton.setStrokeColor(ColorStateList.valueOf(Color.BLACK));

                        params = new RelativeLayout.LayoutParams(mWhiteKeyWidth, mWhiteKeyHeight);
                        params.topMargin = 0;

                        // 算当前八度内白键为第几个
                        int localWhiteIndex = i / 2;
                        if (i >= 5) {
                            ++localWhiteIndex;
                        }

                        params.leftMargin = leftMarginOffset + octave * 7 * mWhiteKeyWidth + localWhiteIndex * mWhiteKeyWidth;
                        Log.d(TAG, String.format("White key %d left margin: %d", midiCode, params.leftMargin));

                        midiKeyboardButton.setLayoutParams(params);
                    }
                    relativeLayout.addView(midiKeyboardButton);
                }
            }
        }
    }

    private static int blackKeyWidth(int whiteKeyWidth) {
        // (3/7/5+4/7/7)/2 * 7 * whiteKeyWidth
        return (int) Math.round(whiteKeyWidth * (0.3 + 2.0 / 7));
    }

    private static int blackKeyHeight(int whiteKeyHeight) {
        return whiteKeyHeight * 3 / 5;
    }

    private static boolean isBlackKey(int midiCode) {
        switch (midiInOctave(midiCode)) {
            case 1:
            case 3:
            case 6:
            case 8:
            case 10:
                return true;
            default:
                return false;
        }
    }

    private static int midiInOctave(int midiCode) {
        return midiCode % 12;
    }

    private static int midiOfOctave(int midiCode) {
        return midiCode / 12 - 1;
    }

    private static char pitchNameChar(int midiInOctave) {
        char name;
        switch (midiInOctave) {
            case 0:
                name = 'C';
                break;
            case 2:
                name = 'D';
                break;
            case 4:
                name = 'E';
                break;
            case 5:
                name = 'F';
                break;
            case 7:
                name = 'G';
                break;
            case 9:
                name = 'A';
                break;
            case 11:
                name = 'B';
                break;
            default:
                name = 0;
                break;
        }
        return name;
    }

    private static String pitchNameString(int midiCode) {
        return pitchNameString(midiCode, true);
    }

    private static String pitchNameString(int midiCode, boolean shouldShowOctave) {
        if (isBlackKey(midiCode)) {
            int leftWhite = midiCode - 1;
            int rightWhite = midiCode + 1;
            return String.format(Locale.getDefault(), "#%s / b%s",
                    pitchNameString(leftWhite, shouldShowOctave),
                    pitchNameString(rightWhite, shouldShowOctave));
        }
        char name = pitchNameChar(midiInOctave(midiCode)); // CDEFGAB
        if (shouldShowOctave) {
            return String.format(Locale.getDefault(), "%c%d", name, midiOfOctave(midiCode));
        }
        return String.format(Locale.getDefault(), "%c", name);
    }

    private interface OnMidiKeyboardListener {
        void onTouch(int midiCode, int velocity);

        void onRelease(int midiCode, int velocity);
    }

    private static class MidiKeyboardButton extends MaterialButton {
        private final int kMidiCode;
        private OnMidiKeyboardListener mListener = null;

        public MidiKeyboardButton(Context context, int midiCode) {
            super(context);
            this.kMidiCode = midiCode;
            int backgroundColor = isBlackKey(midiCode) ? Color.BLACK : Color.WHITE;
            setBackgroundColor(backgroundColor);
            setRippleColor(ColorStateList.valueOf(Color.GRAY));
            setElevation(isBlackKey(midiCode) ? 1 : 0);
            setStateListAnimator(null);
        }

        public void setOnMidiKeyboardListener(OnMidiKeyboardListener e) {
            mListener = e;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            performClick();
            Log.d(TAG, String.format("onTouchEvent: %s %d", pitchNameString(kMidiCode), event.getAction()));
            switch (event.getAction()) {
                default:
                    return false;
                case MotionEvent.ACTION_DOWN:
                    if (mListener != null) {
                        mListener.onTouch(kMidiCode, (int) (event.getPressure() * 127));
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (mListener != null) {
                        mListener.onRelease(kMidiCode, 0);
                    }
                    return true;
            }
        }
    }
}