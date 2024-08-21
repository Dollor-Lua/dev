#include <curses.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <iostream>
#include <memory>
#include <vector>
#include <string>

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
    bool(*achieved)() = nullptr;

    Achievement(std::string title, std::string desc, unsigned int worth) : name(title), description(desc), points(worth) {}
};

class Game {
    private:
    bool mHot = true;

    bool mShouldClose = false;

    std::vector<Achievement> achievementBuffer {
        Achievement("Winner!", "You did something cool or somethin idk", 10)
    };

    void cls() {
        std::cout << "\033[H\033[2J" << std::flush;
    }

    void add(unsigned long long val) {
        data.money += val;
        data.hot = true;
    }

    void redraw() {
        if (!mHot && !data.hot) return;
        mHot = false;
        data.hot = false;

        struct winsize w;
        ioctl(STDOUT_FILENO, TIOCGWINSZ, &w);

        cls();
        std::string str = "Press 'space' to earn money!";
        std::string asstr = "$" + std::to_string(data.money);
        std::string spaces = "";
        for (int i = 0; i < w.ws_col - str.length() - asstr.length(); i++) {
            spaces += " ";
        }

        std::cout << str << spaces << asstr << "\r\n";

        if (achievementBuffer.size() > 0) {
            // move(w.ws_row - 7, 0);
            std::cout << "|------------------------------------------------------------|\r\n";
            int len = 54 - achievementBuffer[0].name.length();
            std::string spaces = "";
            for (int i = 0; i < len / 2; i++) {
                spaces += " ";
            }

            std::cout << "| > " << spaces << achievementBuffer[0].name << spaces << (len % 2 == 1 ? " " : "") << " < |\r\n";
            std::cout << "|                                                            |\r\n";

            std::string cut1 = achievementBuffer[0].description;
            std::string cut2 = "";

            if (achievementBuffer[0].description.length() >= 76) {
                cut1 = achievementBuffer[0].description.substr(0, 76);
                cut2 = achievementBuffer[0].description.substr(76);

                if (cut2.length() >= 76) {
                    cut2 = cut2.substr(0, 73) + "...";
                } else {
                    for (int i = 58 - cut2.length(); i > 0; i--) {
                        cut2 += " ";
                    }
                } 
            } else {
                cut2 = "                                                          ";
                for (int i = 58 - cut1.length(); i > 0; i--) {
                        cut1 += " ";
                    }
            }

            std::cout << "| " << cut1 << " |\r\n";
            std::cout << "| " << cut2 << " |\r\n";
            std::cout << "|------------------------------------------------------------|" << std::endl;
        }
    }

    public:
    Game() {
        initscr(); // init ncurses
        curs_set(0);
    }

    ~Game() {
        curs_set(1);
    }

    void run() {
        while (!mShouldClose) {
            char c = getch();
            switch (c) {
                case '=':
                    mShouldClose = true;
                    break;
                case ' ':
                    add(1);
                    break;
            }

            for (int i = 0; i < achievementBuffer.size(); i++) {
                achievementBuffer[i].cycles += 1;
                if (achievementBuffer[i].cycles >= 35) {
                    achievementBuffer.erase(achievementBuffer.begin() + i);
                    i--;
                    mHot = true;
                }
            }

            redraw();
        }
    }

    friend struct Achievement;
};

int main() {
    std::unique_ptr<Game> game = std::make_unique<Game>();
    game->run();
}