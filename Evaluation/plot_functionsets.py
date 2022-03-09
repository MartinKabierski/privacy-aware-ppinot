import sys

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import seaborn as sns
import pandas as pd
import numpy as np


def plot_functionsets(input):
    font = {'family': 'normal',
            # 'weight' : 'bold',
            'size': 18}

    plt.rc('font', **font)
    plt.rcParams.update({'figure.max_open_warning': 0})
    #sns.set(font_scale=2)
    sns.set_style("ticks")

    data = pd.read_csv(input, sep=";")
    ppis = data["ppi"].unique()

    data['functionset'] = data['functionset'].str.replace('[Standard,','', regex=False)
    data['functionset'] = data['functionset'].str.replace(', Standard]','', regex=False)
    data['functionset'] = data['functionset'].str.replace('Standard,','', regex=False)
    data['functionset'] = data['functionset'].str.replace('Sum_exp','I', regex=False)
    data['functionset'] = data['functionset'].str.replace('Sum_lap','L', regex=False)
    data['functionset'] = data['functionset'].str.replace('SubsampleAggregate','S', regex=False)
    data['functionset'] = data['functionset'].str.replace('S,','S', regex=False)
    data['functionset'] = data['functionset'].str.replace('Sum_exp','I', regex=False)
    data['functionset'] = data['functionset'].str.replace('Sum_lap','L', regex=False)
    data['functionset'] = data['functionset'].str.replace('Maximum_exp','I', regex=False)
    data['functionset'] = data['functionset'].str.replace('Maximum_lap','L', regex=False)
    data['functionset'] = data['functionset'].str.replace('Average_exp','I', regex=False)
    data['functionset'] = data['functionset'].str.replace('Average_lap','L', regex=False)
    data['functionset'] = data['functionset'].str.replace('Sum,','', regex=False)
    data['functionset'] = data['functionset'].str.replace(', Sum','', regex=False)
    data['functionset'] = data['functionset'].str.replace('Sum]','', regex=False)
    data['functionset'] = data['functionset'].str.replace('[','', regex=False)
    data['functionset'] = data['functionset'].str.replace(']','', regex=False)
    data['functionset'] = data['functionset'].str.replace(' ','', regex=False)

    print("Recorded PPIs: " + str(ppis))
    # for each ppi, one plot for each time frame
    for p in ppis:
        print(str(p))
        mask = (data["ppi"] == p)
        ppi_data = data[mask]

        time_frames = ppi_data["from"].unique()
        print("> Time frames: " + str(time_frames))
        functionSets = ppi_data["functionset"].unique()
        print("> Function sets: " + str(functionSets))

        for i, t in enumerate(time_frames):
            print("> " + str(t))
            fig, axs = plt.subplots(1, len(functionSets), sharey=True, sharex=True)
            fig.set_size_inches(4*len(functionSets), 5)

            mask = ((ppi_data["from"] == t))
            ppi_timeframe = ppi_data.loc[mask]

            xmin = np.nanmin(ppi_timeframe["x"])
            xmax = np.nanmax(ppi_timeframe["x"])

            ymax = np.nanmax(ppi_timeframe["y"])

            for j, f in enumerate(functionSets):
                k=0
                if f=="L" or f=="L,L":
                    k=0
                if f=="I" or f=="L,I":
                    k=1
                if f=="I,L":
                    k=2
                if f == "I,I":
                    k = 3
                if f=="S":
                    k=4

                mask = (ppi_data["functionset"] == f)
                selected = ppi_timeframe.loc[mask]
                true_v = selected.iloc[0]["truevalue"]


                fig.title = selected.iloc[0]["ppi"]

                sns.lineplot(data=selected,
                            x="x",
                            y="y",
                            ax=axs[k],
                            #ci=None
                            )

                axs[k].axvline(x=true_v, c="grey", ls="--")
                axs[k].set_xlim(left=xmin
                                , right=xmax+1)
                axs[k].set_ylim(bottom=0.0, top=ymax)
                axs[k].set_title(f + "\n" + "RMSE=" + str("{:.2f}".format(selected.iloc[0]["rmse"])))
            plt.tight_layout()
            plt.savefig(str(p) + "_" + str(t) + ".pdf", format="pdf")
            plt.gcf()
        print()

if __name__ == "__main__":
    plot_functionsets(sys.argv[1])
