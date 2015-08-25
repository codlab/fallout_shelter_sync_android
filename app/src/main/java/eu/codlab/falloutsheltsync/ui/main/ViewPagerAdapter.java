package eu.codlab.falloutsheltsync.ui.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import eu.codlab.falloutsheltsync.ApplicationController;
import eu.codlab.falloutsheltsync.R;

/**
 * Created by kevinleperf on 24/08/15.
 *  
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    CharSequence _titles[];
    SlotFragment[] _fragment;

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);

        _titles = new CharSequence[3];
        _titles[0] = ApplicationController.getInstance().getString(R.string.tab_slot_1);
        _titles[1] = ApplicationController.getInstance().getString(R.string.tab_slot_2);
        _titles[2] = ApplicationController.getInstance().getString(R.string.tab_slot_3);

        _fragment = new SlotFragment[3];
    }

    @Override
    public Fragment getItem(int position) {
        _fragment[position] = SlotFragment.newInstance(position);
        return _fragment[position];
    }

    public boolean isScrolledToTop(int position) {
        if (position < 0 || position > 3 || _fragment[position] == null) return false;
        return _fragment[position].isScrolledToTop();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return _titles[position];
    }

    @Override
    public int getCount() {
        return 3;
    }
}