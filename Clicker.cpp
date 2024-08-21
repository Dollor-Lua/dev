#include <curses.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <chrono>
#include <cmath>
#include <iostream>
#include <memory>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

using mseconds_t = std::chrono::milliseconds;

decltype(mseconds_t().count()) timeSinceEpoch() {
    const auto now = std::chrono::system_clock::now();
    const auto epoch = now.time_since_epoch();
    const auto millis = std::chrono::duration_cast<mseconds_t>(epoch);
    return millis.count();
}

class Game;

struct UData {
    unsigned long long money = 0;

    bool hot = false;
};

UData data;

struct Achievement {
    std::string name = "Unknown";
    std::string description = "You did the unfathomable";
    unsigned int points = 0;
    unsigned int cycles = 0;
    bool (*achieved)() = nullptr;

    Achievement(std::string title, std::string desc, unsigned int worth)
        : name(title), description(desc), points(worth) {}
};

class ShopItem {
  public:
    unsigned int level = 0;

    virtual std::string name() = 0;
    virtual std::string id() = 0;
    virtual unsigned long long cost() = 0;

    virtual void update(std::shared_ptr<Game>){};
    virtual unsigned long long getmps() { return 0; }
    virtual bool isLocked() { return false; }

    unsigned int getLevel() { return level; }
};

class Game {
  private:
    bool mHot = true;

    bool mShouldClose = false;

    bool mShopOpen = false;
    int mShopPos = 0;

    std::vector<Achievement> achievementBuffer{};
    std::vector<std::shared_ptr<ShopItem>> ownedItems{};
    std::unordered_map<std::string, unsigned int> itemLevels;

    void cls() { std::cout << "\033[H\033[2J" << std::flush; }

    std::string rep(std::string str, unsigned int count) {
        if (count == 0)
            return "";

        std::string ret = "";
        for (int i = 0; i < count; i++) {
            ret += str;
        }

        return ret;
    }

    std::string rep(char letter, unsigned int count) { return rep(std::string(1, letter), count); }

    std::string formatNum(unsigned long long num) {
        std::string str = std::to_string(num);

        int n = str.length() - 3;
        int end = (num >= 0) ? 0 : 1;
        while (n > end) {
            str.insert(n, ",");
            n -= 3;
        }

        return str;
    }

    void redraw() {
        if (data.hot) {
            struct winsize w;
            ioctl(STDOUT_FILENO, TIOCGWINSZ, &w);

            data.hot = false;
            std::string asstr = "$" + formatNum(data.money);
            std::cout << "\x1b[0;" << w.ws_col - asstr.length() + 1 << "H" << std::flush << asstr << "\r\n";
        }

        if (!mHot)
            return;
        mHot = false;

        struct winsize w;
        ioctl(STDOUT_FILENO, TIOCGWINSZ, &w);

        cls();

        unsigned long long mps = 0;
        for (const auto &owned : ownedItems) {
            mps += owned->getmps();
        }

        std::string mpsstr = "+$" + formatNum(mps) + "/s";
        std::cout << "\x1b[2;" << w.ws_col - mpsstr.length() + 1 << "H" << std::flush << mpsstr << "\r\n";
        std::cout << "\x1b[1;1H" << std::flush;

        std::string str = "Press 'space' to earn money!";
        std::string asstr = "$" + formatNum(data.money);
        std::string spaces = rep(" ", w.ws_col - str.length() - asstr.length());

        std::cout << str << spaces << asstr << "\r\n";

        std::cout << "\n\r\n";
        if (mShopOpen) {
            std::cout << "╔══════════════════════════════════════╗\r\n";
            std::cout << "║ >> Welcome to the shop!              ║\r\n";
            std::cout << "║  Navigate with the arrow keys, and   ║\r\n";
            std::cout << "║  press the enter key to purchase     ║\r\n";
            std::cout << "║                                      ║\r\n";

            for (int i = 0; i < ownedItems.size(); i++) {
                const auto &item = ownedItems[i];

                std::string v = item->name() + rep(" ", 32 - item->name().length());
                if (i == mShopPos) {
                    v = " > " + v + " < ";
                } else {
                    v = "   " + v + "   ";
                }
                std::cout << "║" << v << "║\r\n";
                std::string costString = "$" + formatNum(item->cost());
                std::cout << "║ " << rep(" ", 36 - costString.length()) << costString << " ║\r\n";
            }

            std::cout << "╚══════════════════════════════════════╝\r\n";
        } else {
            std::cout << "╔══════════════════════════╗\r\n";
            std::cout << "║ Press 'tab' to open shop ║\r\n";
            std::cout << "╚══════════════════════════╝\r\n";
        }

        if (achievementBuffer.size() > 0) {
            move(w.ws_row - 7, 0);
            std::cout << "╔════════════════════════════════════════════════════════════╗\r\n";
            int len = 54 - achievementBuffer[0].name.length();
            std::string spaces = rep(" ", len / 2);

            std::cout << "║ > " << spaces << achievementBuffer[0].name << spaces << rep(" ", len % 2) << " < ║\r\n";
            std::cout << "║                                                            ║\r\n";

            std::string cut1 = achievementBuffer[0].description;
            std::string cut2 = "                           " /* overly long string */ "                               ";

            if (achievementBuffer[0].description.length() >= 76) {
                cut1 = achievementBuffer[0].description.substr(0, 76);
                cut2 = achievementBuffer[0].description.substr(76);

                if (cut2.length() >= 76) {
                    cut2 = cut2.substr(0, 73) + "...";
                } else {
                    cut2 += rep(" ", 58 - cut2.length());
                }
            } else {
                cut1 += rep(" ", 58 - cut1.length());
            }

            std::cout << "║ " << cut1 << " ║\r\n";
            std::cout << "║ " << cut2 << " ║\r\n";
            std::cout << "╚════════════════════════════════════════════════════════════╝" << std::endl;
        }
    }

  public:
    Game() {
        initscr(); // init ncurses
        curs_set(0);
        noecho();
    }

    ~Game() {
        curs_set(1);
        endwin();
    }

    void add(unsigned long long val) {
        data.money += val;
        data.hot = true;
    }

    void run() {
        std::string total = "";
        bool inEscape = false;

        while (!mShouldClose) {
            char c = getch();

            bool skipChecks = false;
            if (inEscape) {
                skipChecks = true;
                switch (c) {
                case 'A':
                    mShopPos--;
                    if (mShopPos < 0)
                        mShopPos = 0;
                    break;
                case 'B':
                    mShopPos++;
                    if (mShopPos > 1)
                        mShopPos = 1;
                    break;
                default:
                    skipChecks = false;
                }

                if (skipChecks) {
                    mHot = true;
                }
            }
            switch (c) {
            case '=':
                mShouldClose = true;
                break;
            case ' ':
                add(itemLevels["ClickPower"] + 1);
                break;
            case '\t':
                mShopOpen = !mShopOpen;
                mHot = true;
                break;
            case '\n':
                if (mShopOpen) {
                    if (ownedItems[mShopPos]->cost() <= data.money) {
                        data.money -= ownedItems[mShopPos]->cost();
                        ownedItems[mShopPos]->level++;
                        itemLevels[ownedItems[mShopPos]->id()] = ownedItems[mShopPos]->getLevel();
                        mHot = true;
                    }
                }
                break;
            case '\033':
                total += c;
                break;
            case '[':
                if (total == "\033") {
                    total += c;
                }
                inEscape = true;
            }
        }
    }

    void setHot(bool hot) { mHot = hot; }

    friend struct Achievement;
    friend class ShopItem;
    friend int main(int argc, const char **argv);
};

class ClickPower : public ShopItem {

  public:
    unsigned int max = 5;

    std::string name() override { return "Stronger click power (Lvl." + std::to_string(level) + ")"; }
    std::string id() override { return "ClickPower"; }

    unsigned long long cost() override {
        if (level < max) {
            return pow(2, level) * 125;
        } else {
            return -1;
        }
    }
};

class MoneyTree : public ShopItem {
  private:
    unsigned long long f = 0;

  public:
    unsigned int max = 50;

    std::string name() override { return "Money tree (Lvl." + std::to_string(level) + ")"; }
    std::string id() override { return "MoneyTree"; }

    unsigned long long cost() override {
        if (level < max) {
            if (level < 10)
                return pow(1.5, level) * 500;
            else if (level < 20)
                return pow(1.85, level) * 550;
            else if (level < 35)
                return pow(2, level) * 600;
            else {
                return pow(2.5, level) * 650;
            }
        } else {
            return -1;
        }
    }

    void update(std::shared_ptr<Game> game) override {
        if ((timeSinceEpoch() - f) * pow(1.42, (double)level) >= 1000) {
            game->add(level);
            f = timeSinceEpoch();
        }
    }

    unsigned long long getmps() override { return level * pow(1.42, (double)level); }
};

int main(int argc, const char **argv) {
    std::shared_ptr<Game> game = std::make_shared<Game>();

    std::shared_ptr<ClickPower> clickPower = std::make_shared<ClickPower>();
    std::shared_ptr<MoneyTree> moneyTree = std::make_shared<MoneyTree>();

    game->ownedItems.push_back(clickPower);
    game->ownedItems.push_back(moneyTree);

    std::thread gameThread([&game]() { game->run(); });

    int64_t start = timeSinceEpoch();

    while (true) {
        for (int i = 0; i < game->achievementBuffer.size(); i++) {
            if (game->achievementBuffer[i].cycles == 0) {
                game->achievementBuffer[i].cycles = timeSinceEpoch();
            }

            if (timeSinceEpoch() - game->achievementBuffer[i].cycles >= 7500) {
                game->achievementBuffer.erase(game->achievementBuffer.begin() + i);
                game->setHot(true);
                i--;
            }
        }

        for (const auto &owned : game->ownedItems) {
            owned->update(game);

            if (!game->itemLevels.count(owned->id())) {
                game->itemLevels[owned->id()] = owned->getLevel();
            }
        }

        game->redraw();

        std::cout << "\x1b[2;0H" << std::flush;

        int64_t elapsed = timeSinceEpoch() - start;
        int64_t seconds = elapsed / 1000;
        int64_t minutes = seconds / 60;
        seconds %= 60;
        int64_t hours = minutes / 60;
        minutes %= 60;

        std::cout << "Time elapsed: " << hours << "h " << minutes << "m " << seconds << "s              " << std::endl;
    }
}