package itba.dreamair2.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import itba.dreamair2.Flight;
import itba.dreamair2.MainActivity;
import itba.dreamair2.R;


public class FlightDetailFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private Flight flight;


    private TextView fromAirport;
    private TextView fromCity;
    private TextView toAirport;
    private TextView toCity;
    private TextView flightNumber;
    private TextView gate;
    private TextView duration;
    private TextView arrivalTime;
    private TextView departureTime;
    private TextView departureDate;



    public FlightDetailFragment() {
        // Required empty public constructor
    }


    public static FlightDetailFragment newInstance(Flight param1) {
        FlightDetailFragment fragment = new FlightDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            flight = (Flight) getArguments().getParcelable(ARG_PARAM1);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_flight_detail, container, false);

        fromAirport= (TextView) view.findViewById(R.id.detail_from_airport);
        fromCity= (TextView) view.findViewById(R.id.detail_from_city);
        toAirport= (TextView) view.findViewById( R.id.detail_to_airport);
        toCity= (TextView) view.findViewById( R.id.detail_to_city);
        flightNumber= (TextView) view.findViewById( R.id.detail_flight_number);
        gate= (TextView) view.findViewById( R.id.detail_gate);
        duration= (TextView) view.findViewById( R.id.detail_duration);
        arrivalTime= (TextView) view.findViewById( R.id.detail_arrival_time);
        departureTime= (TextView) view.findViewById( R.id.detail_departure_time);
        departureDate= (TextView) view.findViewById( R.id.detail_departure_date);
        setFlight(flight);

        gate.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MainActivity activity= (MainActivity) getActivity();
                        activity.addFavoriteFlight(flight);
                        //Snackbar.make(v, "El vuelo se agregó a favoritos" , Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                }
        );
        return view;
    }

    public void setFlight(Flight flight) {
        fromAirport.setText(flight.getDepartureAirportId());
        fromCity.setText(flight.getDepartureCity());
        toAirport.setText(flight.getArrivalAirportId());
        toCity.setText(flight.getArrivalCity());
        flightNumber.setText(flight.getNumber());
        gate.setText(flight.getGate());
        duration.setText(flight.getDuration());
        arrivalTime.setText(flight.getArrivalTime());
        departureTime.setText(flight.getDepartureTime());
        departureDate.setText(flight.getDepartureDate());
    }


}
