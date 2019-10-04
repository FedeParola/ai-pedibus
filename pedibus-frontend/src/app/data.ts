export const routes = [
    {
        id: 0,
        line: {
            id: 10,
            name: 'Line1'
        },
        date: new Date(2019,4,26),
        pathO:[
            {
                id: 100,
                name: 'Nizza',
                stopTime: '10:00',
                passengers:[
                    {
                        name: 'Federico',
                        present: 'false'
                    },
                    {
                        name: 'Andrea',
                        present: 'false'
                    },
                    {
                        name: 'Kamil',
                        present: 'false'
                    }
                ]
            },
            {
                id: 200,
                name: 'Mediterraneo',
                stopTime: '10:10',
                passengers:[
                    {
                        name: 'Luigi',
                        present: 'false'
                    },
                    {
                        name: 'Mario',
                        present: 'false'
                    },
                    {
                        name: 'Giovanni',
                        present: 'false'
                    },
                    {
                        name: 'Piero',
                        present: 'false'
                    },
                    {
                        name: 'Anna',
                        present: 'false'
                    }
                ]
            },
            {
                id: 300,
                name: 'Porta Nuova',
                stopTime: '10:15',
                passengers:[
                    {
                        name: 'Massimo',
                        present: 'false'
                    },
                    {
                        name: 'Giorgia',
                        present: 'false'
                    }
                ]
            }
        ],
        pathR:[
            {
                id: 400,
                name: 'Porta Nuova',
                stopTime: '16:15',
                passengers:[
                    {
                        name: 'Massimo',
                        present: 'false'
                    },
                    {
                        name: 'Giorgia',
                        present: 'false'
                    }
                ]
            },
            {
                id: 500,
                name: 'Mediterraneo',
                stopTime: '16:20',
                passengers:[
                    {
                        name: 'Luigi',
                        present: 'false'
                    },
                    {
                        name: 'Mario',
                        present: 'false'
                    },
                    {
                        name: 'Piero',
                        present: 'false'
                    },
                    {
                        name: 'Anna',
                        present: 'false'
                    },
                    {
                        name: 'Pietro',
                        present: 'false'
                    }
                ]
            },
            {
                id: 600,
                name: 'Nizza',
                stopTime: '16:30',
                passengers:[
                    {
                        name: 'Federico',
                        present: 'false'
                    },
                    {
                        name: 'Andrea',
                        present: 'false'
                    },
                    {
                        name: 'Kamil',
                        present: 'false'
                    }
                ]
            }
        ]
    },
    {
        id: 1,
        line: {
            id: 30,
            name: 'Line3'
        },
        date: new Date(2019,4,30),
        pathO:[
            {
                id: 700,
                name: 'Ferrucci',
                stopTime: '8:00',
                passengers:[
                    {
                        name: 'Gigi',
                        present: 'false'
                    },
                    {
                        name: 'Pippo',
                        present: 'false'
                    },
                    {
                        name: 'Pluto',
                        present: 'false'
                    }
                ]
            },
            {
                id: 800,
                name: 'Sabotino',
                stopTime: '8:20',
                passengers:[
                    {
                        name: 'Maria',
                        present: 'false'
                    },
                    {
                        name: 'Lucia',
                        present: 'false'
                    },
                    {
                        name: 'Davide',
                        present: 'false'
                    }
                ]
            },
            {
                id: 900,
                name: 'Adriano',
                stopTime: '8:35',
                passengers:[
                    {
                        name: 'Giuseppe',
                        present: 'false'
                    },
                    {
                        name: 'Gloria',
                        present: 'false'
                    }
                ]
            }
        ],
        pathR:[
            {
                id: 1000,
                name: 'Adriano',
                stopTime: '18:35',
                passengers:[
                    {
                        name: 'Giuseppe',
                        present: 'false'
                    },
                    {
                        name: 'Gloria',
                        present: 'false'
                    }
                ]
            },
            {
                id: 1100,
                name: 'Sabotino',
                stopTime: '18:40',
                passengers:[
                    {
                        name: 'Maria',
                        present: 'false'
                    },
                    {
                        name: 'Lucia',
                        present: 'false'
                    },
                    {
                        name: 'Davide',
                        present: 'false'
                    }
                ]
            },
            {
                id: 1200,
                name: 'Ferrucci',
                stopTime: '18:50',
                passengers:[
                    {
                        name: 'Gigi',
                        present: 'false'
                    },
                    {
                        name: 'Pippo',
                        present: 'false'
                    },
                    {
                        name: 'Pluto',
                        present: 'false'
                    }
                ]
            }
        ]
    },
    {
        id: 2,
        line: {
            id: 20,
            name: 'Line2'
        },
        date: new Date(2019,5,1),
        pathO:[
            {
                id: 1300,
                name: 'Corso Vittorio',
                stopTime: '8:00',
                passengers:[
                    {
                        name: 'Gigi',
                        present: 'false'
                    },
                    {
                        name: 'Pippo',
                        present: 'false'
                    },
                    {
                        name: 'Pluto',
                        present: 'false'
                    }
                ]
            },
            {
                id: 1400,
                name: 'Corso Castelfidardo',
                stopTime: '8:20',
                passengers:[
                    {
                        name: 'Maria',
                        present: 'false'
                    },
                    {
                        name: 'Lucia',
                        present: 'false'
                    },
                    {
                        name: 'Davide',
                        present: 'false'
                    }
                ]
            },
            {
                id: 1500,
                name: 'Adriano',
                stopTime: '8:35',
                passengers:[
                    {
                        name: 'Giuseppe',
                        present: 'false'
                    },
                    {
                        name: 'Gloria',
                        present: 'false'
                    }
                ]
            }
        ],
        pathR:[
            {
                id: 1600,
                name: 'Adriano',
                stopTime: '18:35',
                passengers:[
                    {
                        name: 'Giuseppe',
                        present: 'false'
                    },
                    {
                        name: 'Gloria',
                        present: 'false'
                    }
                ]
            },
            {
                id: 1700,
                name: 'Corso Castelfidardo',
                stopTime: '18:40',
                passengers:[
                    {
                        name: 'Maria',
                        present: 'false'
                    },
                    {
                        name: 'Lucia',
                        present: 'false'
                    },
                    {
                        name: 'Davide',
                        present: 'false'
                    }
                ]
            },
            {
                id: 1800,
                name: 'Corso Vittorio',
                stopTime: '18:50',
                passengers:[
                    {
                        name: 'Gigi',
                        present: 'false'
                    },
                    {
                        name: 'Pippo',
                        present: 'false'
                    },
                    {
                        name: 'Pluto',
                        present: 'false'
                    }
                ]
            }
        ]
    }
]