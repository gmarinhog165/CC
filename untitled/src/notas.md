
(assumindo que ao criar conexão é possivel saber o IP de quem envia mensagem? ou é necessário enviar source IP?)
Mensagem Pedido de Ficheiro:
    byte[] nome do file
    byte[] checksum SHA-1

Mensagem Resposta a Pedido:
    int numchunks
    byte[] converted Hash<Byte[](ip's), int[](numero dos chunks)> -> criado pelo algoritmo
    byte[] checksum SHA-1

Mensagem ao Tracker de Sucesso after get chunk:
    byte[] nome do file
    int num_do_chunk
    byte[] checksum SHA-1


(criar interface SHA-1?)



FS_Node:
    Hash<String(nome file), Chunk[]>


    Métodos:
        byte[] fileToBytes(string path) -> é preciso guardar a .ext

        int numChunks(int length_data) -> diz o número de chunks que um dado file precisa

        Chunk[] byteToChunks(byte[] data) -> converte byte[] para chunk[]

        void createEntry() -> depois de saber o file que quer, com o respetivo número de chunks, cria a entrada
    na hash inicializando o Chunk[] a null em todos de forma a que os chunks fiquem no sítio correto.

        int offsetToIndex(Chunk bloco) -> a partir do offset de um dado chunk calcula a posição no array para ser adicionado

        void addChunkToHash() -> adiciona o chunk à hash no seu respetivo index

        void enviaPedidoFile() -> quando um node quer um file

        void enviaFile() -> para outro node

        void enviaMsgSucesso() -> envia para o tracker quando recebe um chunk com sucesso


FS_Tracker:
    Hash<String(nome file), Hash<int(numero do chunk), List<byte[]>(endereços IP)>>

    Métodos:
        algoritmo() -> para escolher a quem vai buscar cenas

        contaNumChunks() -> fazer stream e contar o número de keys da 2nd hash

        atualizaHash() -> quando recebe mensagem de Sucesso

        enviaRespostaPedido() -> quando um node pede um file



Msg 2:
    Node:

    -> Nome Ficheiro byte[]
    -> length int
    -> nº de chunks int
    -> tipo de msg byte

    Tracker:

    -> escreve na hash


Msg 3:
    Node:

    -> Nome file byte[]
    -> Length int
    -> tipo de msg byte

    Tracker:

    -> read da hash


Msg 4:
    Tracker:

    -> hash byte[]
    -> length int
    -> tipo msg byte

    Node:
