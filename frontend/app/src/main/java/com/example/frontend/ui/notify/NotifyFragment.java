package com.example.frontend.ui.notify;
import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import androidx.fragment.app.Fragment; import com.example.frontend.R;
public class NotifyFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notify, container, false);
    }
}