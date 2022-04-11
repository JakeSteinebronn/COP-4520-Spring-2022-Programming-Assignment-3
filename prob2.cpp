#include <bits/stdc++.h>
#include <chrono>
#include <atomic>
#include <thread>
#include <mutex>
#include <random>

using namespace std;

#define rep(i, a, b) for(int i = a; i < (b); ++i)
#define all(x) begin(x), end(x)
#define sz(x) (int)(x).size()
typedef long long int ll;
typedef long double ld;
typedef pair<int, int> pii;
typedef vector<int> vi;

typedef pair<ll, int> reading;

ll timeMillis() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}

reading readings[100000];
atomic_int ri{0};

int done = 0;

const ll ONE_MINUTE = 50;
const ll TEN_MINUTES = ONE_MINUTE * 10;
const ll ONE_HOUR = ONE_MINUTE * 60;


// Make a measurement with mean -15 stdev 20 so it's not just -100, 70 every time
std::default_random_engine generator (0);
std::normal_distribution<double> distribution (-15,20);

int measurement() {
    int res = int(distribution(generator));
    return min(70, max(-100, res));
}

int num_hours = 5;

int main() {
    vector<thread> vec;

    ll startTime = timeMillis();
    auto sensorEx = [&](int tnum) -> void {
        while(!done){
            readings[ri++] = {timeMillis(), measurement()};
            std::this_thread::sleep_for(std::chrono::milliseconds(ONE_MINUTE));
        }
    };

    int hourNum = 1;
    auto mainEx = [&]() -> void {
        
        while(!done){
            ll win_start = timeMillis();
            std::this_thread::sleep_for(std::chrono::milliseconds(ONE_HOUR));
            if(done) break;
            vector<pair<int, int>> windows(6, {1000, -1000});

            for(int i = 0; i < int(ri); i++){
                ll diff = readings[i].first - win_start;
                if(diff < 0 || diff >= ONE_HOUR) continue;
                int j = diff / TEN_MINUTES;
                assert(0 <= j && j <= 5);
                windows[j].first = min(windows[j].first, readings[i].second);
                windows[j].second = max(windows[j].second, readings[i].second);
            }

            int mintemp = 1000, maxtemp = -1000, maxdiff = 0;
            for(auto [lo, hi] : windows) 
                mintemp = min(mintemp, lo),
                maxtemp = max(maxtemp, hi),
                maxdiff = max(maxdiff, hi-lo);
            
            stringstream res;
            res << "Report for hour " << hourNum++ << "/" << num_hours << ": \n";
            res << "Lowest temp: " << mintemp << "\nHighest temp: " << maxtemp << "\nHighest 10-minute difference: " << maxdiff << "\n\n";
            cout << res.str();
            cout.flush();
        }
    };

    rep(i,0,8) vec.emplace_back(sensorEx, i);
    vec.emplace_back(mainEx);

    while(hourNum <= num_hours) {
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

    done = 1;

    for(auto &t : vec) t.join();

    return 0;
}