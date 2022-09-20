import sys
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

def plot_optimal_functionsets(input):
    font = {'family': 'normal',
            # 'weight' : 'bold',
            'size': 18}

    plt.rc('font', **font)
    plt.rcParams.update({'figure.max_open_warning': 0})
    sns.set_style("ticks")

    data = pd.read_csv(input, sep=";")
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

    ppis = data["ppi"].unique()
    print("Recorded PPIs: " + str(ppis))

    for p in ppis:
        print(str(p))
        mask = (data["ppi"] == p)
        ppi_data = data[mask]

        time_frames = ppi_data["from"].unique()
        print("> Time frames: " + str(time_frames))
        functionSets = ppi_data["functionset"].unique()
        print("> Function sets: " + str(functionSets))

        fig, axs = plt.subplots(1, len(time_frames), sharey=True, sharex=True)
        fig.set_size_inches(4*len(time_frames), 5)

        for j, t in enumerate(time_frames):
            print("> " + str(t))
            mask = (ppi_data["from"] == t)
            timeframe = ppi_data.loc[mask]

            # TODO this most likely has a better way to do it
            #find function set with lowest rsme
            opt_function = ""
            opt_rmse = 10000000000000000

            for f in timeframe["functionset"].unique():
                mask = timeframe["functionset"] == f
                func_timeframe = timeframe.loc[mask]
                rmse = func_timeframe.iloc[0]["rmse"]
                if rmse < opt_rmse:
                    opt_rmse = rmse
                    opt_function = f

            mask = ((ppi_data["from"] == t) & (ppi_data["functionset"] == opt_function))
            selected = ppi_data.loc[mask]

            sns.lineplot(data=selected,
                        x="x",
                        y="y", ax=axs[j],
                        )

            true_v = selected.iloc[0]["truevalue"]
            axs[j].axvline(x=true_v, c="grey",ls="--")

            axs[j].set_title("\n" + opt_function + "\n" + "RMSE=" + str(
                "{:.2f}".format(selected.iloc[0]["rmse"])))
            #fig.suptitle(p)

        for j, t in enumerate(time_frames):
            axs[j].set_ylim(bottom=0.0)

        plt.tight_layout()
        plt.savefig(str(p)+"_optimal.pdf", format="pdf")
        plt.gcf()
        print()

if __name__ == "__main__":
    plot_optimal_functionsets(sys.argv[1])
