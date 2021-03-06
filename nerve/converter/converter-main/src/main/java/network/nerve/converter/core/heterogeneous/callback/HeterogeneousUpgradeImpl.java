/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.converter.core.heterogeneous.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IHeterogeneousUpgrade;
import network.nerve.converter.core.heterogeneous.callback.management.CallBackBeanManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.storage.VirtualBankStorageService;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.ArrayList;
import java.util.List;

import static network.nerve.converter.config.ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST;

/**
 * 合约升级后，异构链组件调用CORE，切换docking实例
 * @author: Mimi
 * @date: 2020-08-31
 */
public class HeterogeneousUpgradeImpl implements IHeterogeneousUpgrade {
    private Chain nerveChain;
    /**
     * 异构链chainId
     */
    private int hChainId;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private VirtualBankStorageService virtualBankStorageService;

    public HeterogeneousUpgradeImpl(Chain nerveChain, int hChainId, CallBackBeanManager callBackBeanManager) {
        this.nerveChain = nerveChain;
        this.hChainId = hChainId;
        this.heterogeneousDockingManager = callBackBeanManager.getHeterogeneousDockingManager();
        this.virtualBankStorageService = callBackBeanManager.getVirtualBankStorageService();
    }

    @Override
    public void switchDocking(IHeterogeneousChainDocking newDocking) {
        nerveChain.getLogger().info("合约升级，调用流程切换");
        this.heterogeneousDockingManager.registerHeterogeneousDocking(hChainId, newDocking);
        virtualBankUpgradeProcess(newDocking.version());
    }

    private void virtualBankUpgradeProcess(int version){
        nerveChain.setCurrentHeterogeneousVersion(version);
        ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT = INIT_VIRTUAL_BANK_PUBKEY_LIST.size();
        ConverterContext.VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED =
                ConverterContext.VIRTUAL_BANK_AGENT_TOTAL - ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT;

        // 版本升级 从虚拟银行移除非配置的种子节点成员
        List<VirtualBankDirector> listOutDirector = new ArrayList<>();
        for(VirtualBankDirector director : nerveChain.getMapVirtualBank().values()) {
            if(!director.getSeedNode()){
                continue;
            }
            boolean rs = false;
            for(String pubkey : INIT_VIRTUAL_BANK_PUBKEY_LIST){
                if(pubkey.equals(director.getSignAddrPubKey())){
                    rs = true;
                }
            }
            if(!rs){
                listOutDirector.add(director);
            }
        }
        for(VirtualBankDirector outDirector : listOutDirector){
            // 如果踢出了的银行节点是当前节点, 则修改状态
           if(VirtualBankUtil.isCurrentDirector(nerveChain)){
               SignAccountDTO info = VirtualBankUtil.getCurrentDirectorSignInfo(nerveChain);
               if(outDirector.getSignAddress().equals(info.getAddress())){
                   nerveChain.getCurrentIsDirector().set(false);
               }
           }
        }
        // 移除时更新顺序
        VirtualBankUtil.virtualBankRemove(nerveChain, nerveChain.getMapVirtualBank(), listOutDirector, virtualBankStorageService);
        try {
            nerveChain.getLogger().info("异构链组件版本切换完成, 当前异构链版本:{}, 当前虚拟银行成员:{}",
                    version, JSONUtils.obj2json(nerveChain.getMapVirtualBank()));
        } catch (JsonProcessingException e) {
            nerveChain.getLogger().warn("MapVirtualBank log print error ");
        }
    }
}
