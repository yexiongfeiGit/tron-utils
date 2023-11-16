package com.wokoworks;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.abi.FunctionReturnDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.Utils;
import org.tron.trident.abi.datatypes.*;
import org.tron.trident.api.GrpcAPI;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Demo {
    @Test
    public void testGetNowBlock2() {
        ApiWrapper client = new ApiWrapper("http://127.0.0.2545", "", "");
        Response.BlockExtention block = client.blockingStub.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
        System.out.println("块高度: "+block.getBlockHeader().getRawData().getNumber());
        System.out.println("块哈希: "+Hex.toHexString(block.getBlockid().toByteArray()));
        System.out.println("块父哈希: "+Hex.toHexString(block.getBlockHeader().getRawData().getParentHash().toByteArray()));
    }

    @Test
    public void testTransaction() throws InvalidProtocolBufferException {
        ApiWrapper client = ApiWrapper.ofShasta("");
        ByteString byteString = ByteString.copyFrom(Hex.decode("959563f6e75c6523d5f7eccda3824954d03b51bc99ab93ef648f8b9c38fc95cb"));
        GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder().setValue(byteString).build();

        final Chain.Transaction transaction = client.blockingStub.getTransactionById(bytesMessage);

        for (Chain.Transaction.Contract contract : transaction.getRawData().getContractList()) {
            if ("type.googleapis.com/protocol.TransferContract".equals(contract.getParameter().getTypeUrl())) {

                final Contract.TransferContract unpack = contract.getParameter().unpack(Contract.TransferContract.class);
                final ByteString ownerAddress = unpack.getOwnerAddress();

                final String fromAddress = Base58Check.bytesToBase58(ownerAddress.toByteArray());
                final String toAddress = Base58Check.bytesToBase58(unpack.getToAddress().toByteArray());
                System.out.println("转出地址: "+fromAddress);
                System.out.println("接收地址: "+toAddress);
                System.out.println("接收数量: "+fromWei(new BigDecimal(unpack.getAmount()), 6).toPlainString()+"TRX");
            }
        }

    }

    @Test
    public void testBlockTransaction() {
        ApiWrapper client = ApiWrapper.ofShasta("");
        GrpcAPI.NumberMessage numberMessage = GrpcAPI.NumberMessage.newBuilder().setNum(18661472).build();
        final List<Response.TransactionInfo> transactionInfoList = client.blockingStub.getTransactionInfoByBlockNum(numberMessage).getTransactionInfoList();

        for (Response.TransactionInfo transactionInfo : transactionInfoList) {
            System.out.println("交易哈希:"+Hex.toHexString(transactionInfo.getId().toByteArray()));

            if (transactionInfo.getLogList().size() <= 0) {
                //testTransaction() 解析这个交易的TRX
                continue;
            }

            for (Response.TransactionInfo.Log log : transactionInfo.getLogList()) {
                final ByteString topics = log.getTopics(0);

                // Transfer(address,address,uint256)
                if ("ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef".equals(Hex.toHexString(topics.toByteArray()))) {

                    Function function = new Function("decimals", new ArrayList<>() , ImmutableList.of(TypeReference.create(Uint.class)));
                    String data = FunctionEncoder.encode(function);
                    final ByteString bytes = ByteString.copyFrom(Hex.decode(data));

                    Contract.TriggerSmartContract request = Contract.TriggerSmartContract.newBuilder()
                            .setContractAddress(transactionInfo.getContractAddress())
                            .setData(bytes).build();
                    final Response.TransactionExtention transactionExtention = client.blockingStub.triggerConstantContract(request);
                    final BigInteger decimals = new BigInteger(transactionExtention.getConstantResult(0).toByteArray());
                    System.out.println("decimals: "+new BigDecimal(decimals).toPlainString());

                    System.out.println("转出地址: "+ Base58Check.bytesToBase58(log.getTopics(1).toByteArray()));
                    System.out.println("接收地址: "+Base58Check.bytesToBase58(log.getTopics(2).toByteArray()));
                    BigInteger bigInteger = new BigInteger(log.getData().toByteArray());
                    System.out.println("接收数量:"+fromWei(new BigDecimal(bigInteger), decimals.intValue()).toPlainString());

                    Function functionSymbol = new Function("symbol", new ArrayList<>() , ImmutableList.of(TypeReference.create(Uint.class)));
                    String dataSymbol = FunctionEncoder.encode(functionSymbol);
                    final ByteString bytesSymbol = ByteString.copyFrom(Hex.decode(dataSymbol));
                    Contract.TriggerSmartContract requestSymbol = Contract.TriggerSmartContract.newBuilder()
                            .setContractAddress(transactionInfo.getContractAddress())
                            .setData(bytesSymbol).build();
                    final Response.TransactionExtention transactionExtentionSymbol = client.blockingStub.triggerConstantContract(requestSymbol);
                    final byte[] resultBytesSymbol = transactionExtentionSymbol.getConstantResult(0).toByteArray();

                    List<Byte> byteList = new ArrayList<>();
                    for (int i = resultBytesSymbol.length-1; i >= 0; i--) {
                        if (resultBytesSymbol[i] != 0) {
                            byteList.add(resultBytesSymbol[i]);
                        }
                    }
                    byte[] bys = new byte[byteList.size()];
                    for (int i = 0; i < bys.length; i++) {
                        bys[i] = byteList.get(i);
                    }
                    System.out.println("币种: "+ new String(bys));
                }
            }
        }
    }

    private BigDecimal fromWei(BigDecimal number, int decimal) {
        return number.divide(BigDecimal.TEN.pow(decimal), 18, RoundingMode.DOWN);
    }
}
