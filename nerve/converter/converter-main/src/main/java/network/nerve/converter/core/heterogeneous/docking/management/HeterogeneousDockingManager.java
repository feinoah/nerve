/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.converter.core.heterogeneous.docking.management;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.heterogeneouschain.ht.constant.HtConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.rpc.call.AccountCall;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static network.nerve.converter.config.ConverterContext.HUOBI_CROSS_CHAIN_HEIGHT;
import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
@Component
public class HeterogeneousDockingManager {

    /**
     * 管理每个异构链组件的接口实现实例
     */
    private Map<Integer, IHeterogeneousChainDocking> heterogeneousDockingMap = new ConcurrentHashMap<>();

    private boolean huobiCrossChainAvailable = false;

    public void registerHeterogeneousDocking(int heterogeneousChainId, IHeterogeneousChainDocking docking) {
        heterogeneousDockingMap.put(heterogeneousChainId, docking);
    }

    public IHeterogeneousChainDocking getHeterogeneousDocking(int heterogeneousChainId) throws NulsException {
        // 增加HT跨链的生效高度
        /*if (LATEST_BLOCK_HEIGHT < HUOBI_CROSS_CHAIN_HEIGHT && heterogeneousChainId == HtConstant.HT_CHAIN_ID) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR, String.format("error heterogeneousChainId: %s", heterogeneousChainId));
        }*/
        IHeterogeneousChainDocking docking = heterogeneousDockingMap.get(heterogeneousChainId);
        if (docking == null) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR, String.format("error heterogeneousChainId: %s", heterogeneousChainId));
        }
        return docking;
    }

    public Collection<IHeterogeneousChainDocking> getAllHeterogeneousDocking() {
        // 增加HT跨链的生效高度
        if (LATEST_BLOCK_HEIGHT < HUOBI_CROSS_CHAIN_HEIGHT && heterogeneousDockingMap.containsKey(HtConstant.HT_CHAIN_ID)) {
            Map<Integer, IHeterogeneousChainDocking> result = new HashMap<>();
            result.putAll(heterogeneousDockingMap);
            result.remove(HtConstant.HT_CHAIN_ID);
            return result.values();
        }
        return heterogeneousDockingMap.values();
    }

    public void checkAccountImportedInDocking(Chain chain, SignAccountDTO signAccountDTO) {
        if (!huobiCrossChainAvailable && LATEST_BLOCK_HEIGHT >= HUOBI_CROSS_CHAIN_HEIGHT) {
            // 向HT异构链组件,注册地址签名信息
            try {
                // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
                if (null != signAccountDTO) {
                    String priKey = AccountCall.getPriKey(signAccountDTO.getAddress(), signAccountDTO.getPassword());
                    // 向HT跨链组件导入账户
                    IHeterogeneousChainDocking dock = this.getHeterogeneousDocking(HtConstant.HT_CHAIN_ID);
                    if (dock != null && dock.getCurrentSignAddress() == null) {
                        dock.importAccountByPriKey(priKey, signAccountDTO.getPassword());
                        chain.getLogger().info("[初始化]本节点是虚拟银行节点,向异构链组件注册签名账户信息..");
                    }
                }
                huobiCrossChainAvailable = true;
            } catch (NulsException e) {
                chain.getLogger().warn("向异构链组件注册地址签名信息异常, 错误: {}", e.format());
            }
        }
    }
}
