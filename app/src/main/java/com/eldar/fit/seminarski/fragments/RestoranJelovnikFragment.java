package com.eldar.fit.seminarski.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eldar.fit.seminarski.R;
import com.eldar.fit.seminarski.data.HranaItemVM;
import com.eldar.fit.seminarski.data.HranaPrikazVM;
import com.eldar.fit.seminarski.data.Korpa;
import com.eldar.fit.seminarski.data.KorpaHranaStavka;
import com.eldar.fit.seminarski.helper.MyAbstractRunnable;
import com.eldar.fit.seminarski.helper.MyApiRequest;
import com.eldar.fit.seminarski.helper.MySession;
import com.eldar.fit.seminarski.data.RestoranInfo;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RestoranJelovnikFragment extends Fragment {
    public static String Tag = "restoranJelovnikFragment";

    private static final String JELOVNIK_RESTORANA = "jelovnikRestorana";

    private Korpa korpa;
    private RestoranInfo restoran;

    private ListView listViewHrana;
    private List<HranaItemVM> podaci;
    private List<HranaItemVM> initialPodaci;
    private BaseAdapter listHranaAdapter;

    private ProgressBar progressBar_hranaList;
    private TextView restoranJelovnikNoData;
    private ImageButton btnJelovnikClose;
    private TextView titleKorpaNaziv;

    public static RestoranJelovnikFragment newInstance(RestoranInfo restoran) {
        RestoranJelovnikFragment fragment = new RestoranJelovnikFragment();

        Bundle args = new Bundle();
        args.putSerializable(JELOVNIK_RESTORANA, restoran);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(JELOVNIK_RESTORANA)) {
            restoran = (RestoranInfo) getArguments().getSerializable(JELOVNIK_RESTORANA);
        }
    }

    @Override
    public void onResume() {
        getKorpaSession();
        super.onResume();
    }

    private void getKorpaSession() {
        if (MySession.getKorpa() == null) {
            MySession.setKorpa(new Korpa());
        }
        korpa = MySession.getKorpa();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getKorpaSession();
        View view = inflater.inflate(R.layout.restoran_jelovnik_fragment, container, false);

        titleKorpaNaziv = view.findViewById(R.id.titleKorpaNaziv);
        titleKorpaNaziv.setText(getString(R.string.restoran_jelovnik_toolbar_title, restoran.getNaziv()));

        progressBar_hranaList = view.findViewById(R.id.progressBar_hranaList);

        btnJelovnikClose = view.findViewById(R.id.btnJelovnikClose);
        btnJelovnikClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    getActivity().onBackPressed();
                } catch (NullPointerException e) {
                    //e.printStackTrace();
                }
            }
        });

        restoranJelovnikNoData = view.findViewById(R.id.restoranJelovnikNoData);

        SearchView searchViewPretraga = view.findViewById(R.id.searchViewPretraga);
        searchViewPretraga.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterListViewHrana(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterListViewHrana(newText);
                return true;
            }
        });

        listViewHrana = view.findViewById(R.id.listViewHrana);

        MyApiRequest.get(String.format(MyApiRequest.ENDPOINT_RESTORANI_HRANA, restoran.getId()),
                new MyAbstractRunnable<HranaPrikazVM>() {
            @Override
            public void run(HranaPrikazVM hranaPrikazVM) {
                onHranaListReceived(hranaPrikazVM, null, null);
            }

            @Override
            public void error(@Nullable Integer statusCode, @Nullable String errorMessage) {
                onHranaListReceived(null, statusCode, errorMessage);
            }
        });

        return view;
    }

    private void onHranaListReceived(@Nullable HranaPrikazVM hranaPodaci, @Nullable Integer statusCode, @Nullable String errorMessage) {
        progressBar_hranaList.setVisibility(View.INVISIBLE);

        int noDataTextVisibility = hranaPodaci != null && hranaPodaci.hrana != null && hranaPodaci.hrana.size() > 0 ? View.INVISIBLE: View.VISIBLE;
        restoranJelovnikNoData.setVisibility(noDataTextVisibility);

        if (hranaPodaci != null) {
            podaci = initialPodaci = hranaPodaci.hrana;
            popuniPodatke();
        } else {
            Snackbar.make(getActivity().findViewById(R.id.fragmentContainer),
                    getString(R.string.dogodila_se_greska),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void filterListViewHrana(String query) {
        try {
            List<HranaItemVM> filtered = new ArrayList<HranaItemVM>();

            for (HranaItemVM n: initialPodaci) {
                if (n.getSearchIndex().contains(query.toLowerCase())) {
                    filtered.add(n);
                }
            }

            podaci = filtered;
            listHranaAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void popuniPodatke() {
        listHranaAdapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return podaci.size();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return getCount();
            }

            @Override
            public int getItemViewType(int position) {
                return position;
            }

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = getLayoutInflater();
                    view = inflater != null ? inflater.inflate(R.layout.restoran_jelovnik_stavka, parent, false) : null;
                }

                HranaItemVM hranaStavka = podaci.get(position);

                // Main row
                LinearLayout stavkaJelovnikRoot = view.findViewById(R.id.stavkaJelovnikRoot);

                TextView textStavkaJelovnikNaziv = view.findViewById(R.id.textStavkaJelovnikNaziv);
                textStavkaJelovnikNaziv.setText(hranaStavka.getNaziv());

                TextView textStavkaJelovnikOpis = view.findViewById(R.id.textStavkaJelovnikOpis);
                textStavkaJelovnikOpis.setText(hranaStavka.getOpis());

                TextView textStavkaJelovnikCijena = view.findViewById(R.id.textStavkaJelovnikCijena);
                textStavkaJelovnikCijena.setText(getString(R.string.cijena_double, hranaStavka.getCijena()));

                CircleImageView imageStavkaJelovnikSlika = view.findViewById(R.id.imageStavkaJelovnikSlika);
                Glide.with(getActivity())
                        .load(hranaStavka.getImageUrl())
                        .centerCrop()
                        .into(imageStavkaJelovnikSlika);

                // Hidden status row (prikazi samo ako je stavka u korpi)
                LinearLayout jelovnikStavkaAddedStatsHolder = view.findViewById(R.id.jelovnikStavkaAddedStatsHolder);
                TextView textStavkaJelovnikStats = view.findViewById(R.id.textStavkaJelovnikStats);

                // Dodaj-ukloni stavku Btns
                ImageButton btnDodajKorpaStavku = view.findViewById(R.id.btnDodajKorpaStavku);
                btnDodajKorpaStavku.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        KorpaHranaStavka s = do_dodajStavku(hranaStavka);
                        ripple(stavkaJelovnikRoot);
                        updateStatsUI(s,  jelovnikStavkaAddedStatsHolder, textStavkaJelovnikStats, btnDodajKorpaStavku);
                    }
                });

                ImageButton btnUkloniKorpaStavku = view.findViewById(R.id.btnUkloniKorpaStavku);
                btnUkloniKorpaStavku.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        KorpaHranaStavka s = do_ukloniStavku(hranaStavka);
                        ripple(stavkaJelovnikRoot);
                        updateStatsUI(s,  jelovnikStavkaAddedStatsHolder, textStavkaJelovnikStats, btnDodajKorpaStavku);
                    }
                });

                // Provjeri da li je stavka već u korpi
                KorpaHranaStavka korpaStavka = korpa.getStavka(hranaStavka);
                if (korpaStavka != null) {
                    updateStatsUI(korpaStavka, jelovnikStavkaAddedStatsHolder, textStavkaJelovnikStats, btnDodajKorpaStavku);
                }

                return view;
            }
        };

        listViewHrana.setAdapter(listHranaAdapter);
    }

    private KorpaHranaStavka do_dodajStavku(HranaItemVM stavka) {
        KorpaHranaStavka s = korpa.dodajStavku(stavka);
        MySession.setKorpa(korpa);
        return s;
    }

    private KorpaHranaStavka do_ukloniStavku(HranaItemVM stavka) {
        KorpaHranaStavka s = korpa.ukloniStavku(stavka);
        MySession.setKorpa(korpa);
        return s;
    }

    private void ripple(LinearLayout container) {
        container.setPressed(true);
        container.setPressed(false);
    }

    private void updateStatsUI(KorpaHranaStavka korpaStavka, LinearLayout statsContainer, TextView stanje, ImageButton btn) {
        if (korpa.getHranaStavke().contains(korpaStavka)) {
            stanje.setText(getString(R.string.korpa_stavka_stats_stanje, korpaStavka.getKolicina(), korpaStavka.getUkupnaCijena()));
            btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_plus_round_added));

            statsContainer.setVisibility(View.VISIBLE);
        } else {
            btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_plus_round));
            statsContainer.setVisibility(View.GONE);
        }
    }
}
