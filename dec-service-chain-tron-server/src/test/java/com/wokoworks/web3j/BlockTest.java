//package com.wokoworks.web3j;
//
//import com.wokoworks.TestApplication;
//import com.wokoworks.service.impl.HandleNewHeadService;
//import lombok.extern.slf4j.Slf4j;
////import org.junit.BeforeClass;
////import org.junit.Test;
//import org.springframework.beans.factory.BeanFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.web3j.protocol.core.methods.response.EthBlock;
//
////@Ignore
//@Slf4j
//public class BlockTest extends TestApplication {
//
//	//private static Web3j web3j;
//	@Autowired
//	private BeanFactory beanFactory;
//
//	//@BeforeClass
//	public static void init() {
//		//web3j = Web3j.build(new HttpService("http://54.180.118.213:9000"));
//	}
//
//	//@Test
//	public void test() {
//
//		EthBlock.Block block = new EthBlock.Block();
//		block.setParentHash("bbb");
//		block.setHash("aaa");
//		block.setNumber("111");
//		HandleNewHeadService handleNewHeadService = null;//new HandleBlockService(beanFactory, null, "");
//
//		int count = 10;
//		for (int i = 0; i < count; i++) {
////			ThreadPoolUtil.HANDLE_TRANSACTION.execute(new Runnable() {
////				@Override
////				public void run() {
////					//handleBlockService.handleBlock(block);
////				}
////			});
//		}
//	}
//}
